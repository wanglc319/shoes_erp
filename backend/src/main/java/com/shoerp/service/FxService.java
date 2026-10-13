package com.shoerp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.shoerp.common.BusinessException;

@Service
public class FxService {

    private static final List<String> SUPPORTED = List.of(
            "USD", "EUR", "CNY", "GBP", "AUD", "CAD", "JPY", "HKD", "SGD", "KRW", "INR", "CHF");

    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;

    private final RestClient restClient;

    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    private record CachedRates(Map<String, BigDecimal> rates, String updatedAt, long fetchedAt, String source) {
    }

    public FxService() {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(3))
                .withReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }

    public Map<String, Object> rates(String base) {
        String code = normalize(base);
        CachedRates cached = cache.get(code);
        boolean stale = true;
        String notice = null;
        if (cached == null || System.currentTimeMillis() - cached.fetchedAt() > CACHE_TTL_MILLIS) {
            CachedRates fresh = fetch(code);
            if (fresh != null) {
                cached = fresh;
                cache.put(code, cached);
                stale = false;
            } else if (cached != null) {
                notice = "stale";
            }
        } else {
            stale = false;
        }
        if (cached == null) {
            throw new BusinessException(503, "Failed to fetch exchange rates");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("base", code);
        result.put("rates", cached.rates());
        result.put("updatedAt", cached.updatedAt());
        result.put("stale", stale);
        result.put("notice", notice);
        result.put("source", cached.source());
        return result;
    }

    /** Try multiple rate providers in order; first success wins. */
    private CachedRates fetch(String base) {
        for (Provider provider : providers) {
            try {
                Map<String, BigDecimal> rates = provider.fetch(base);
                if (!rates.isEmpty()) {
                    return new CachedRates(rates, Instant.now().toString(), System.currentTimeMillis(), provider.name());
                }
            } catch (Exception ignored) {
                // try next provider
            }
        }
        return null;
    }

    private interface Provider {
        String name();

        Map<String, BigDecimal> fetch(String base) throws Exception;
    }

    private final Provider fromErApi = new Provider() {
        @Override
        public String name() {
            return "open.er-api.com";
        }

        @Override
        public Map<String, BigDecimal> fetch(String base) throws Exception {
            Map<?, ?> body = restClient.get()
                    .uri("https://open.er-api.com/v6/latest/{base}", base)
                    .retrieve()
                    .body(Map.class);
            return pickSupported(body == null ? null : (Map<?, ?>) body.get("rates"));
        }
    };

    private final Provider fromJsDelivr = new Provider() {
        @Override
        public String name() {
            return "jsdelivr currency-api";
        }

        @Override
        public Map<String, BigDecimal> fetch(String base) throws Exception {
            Map<?, ?> body = restClient.get()
                    .uri("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/{code}.json",
                            base.toLowerCase())
                    .retrieve()
                    .body(Map.class);
            return pickSupported(body == null ? null : (Map<?, ?>) body.get(base.toLowerCase()));
        }
    };

    private final Provider fromFrankfurter = new Provider() {
        @Override
        public String name() {
            return "frankfurter.dev";
        }

        @Override
        public Map<String, BigDecimal> fetch(String base) throws Exception {
            Map<?, ?> body = restClient.get()
                    .uri("https://api.frankfurter.dev/v1/latest?base={base}", base)
                    .retrieve()
                    .body(Map.class);
            return pickSupported(body == null ? null : (Map<?, ?>) body.get("rates"));
        }
    };

    private final List<Provider> providers = List.of(fromErApi, fromJsDelivr, fromFrankfurter);

    /** Keep only supported currencies; tolerates upper- or lower-case keys. */
    private Map<String, BigDecimal> pickSupported(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<String, BigDecimal> parsed = new TreeMap<>();
        for (String target : SUPPORTED) {
            Object value = raw.get(target);
            if (value == null) {
                value = raw.get(target.toLowerCase());
            }
            if (value != null) {
                try {
                    parsed.put(target, new BigDecimal(value.toString()));
                } catch (NumberFormatException ignored) {
                    // skip malformed entry
                }
            }
        }
        return parsed;
    }

    public Map<String, Object> convert(String from, String to, BigDecimal amount) {
        String source = normalize(from);
        String target = normalize(to);
        BigDecimal value = amount == null ? BigDecimal.ONE : amount;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(400, "Amount must not be negative");
        }
        BigDecimal rate = rate(source, target);
        BigDecimal converted = value.multiply(rate).setScale(4, RoundingMode.HALF_UP);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", source);
        result.put("to", target);
        result.put("amount", value);
        result.put("rate", rate);
        result.put("converted", converted);
        return result;
    }

    private BigDecimal rate(String from, String to) {
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }
        Map<String, Object> data = rates(from);
        Map<String, BigDecimal> table = castRates(data.get("rates"));
        BigDecimal rate = table.get(to);
        if (rate == null) {
            throw new BusinessException(400, "Unsupported currency pair: " + from + "/" + to);
        }
        return rate;
    }

    @SuppressWarnings("unchecked")
    private Map<String, BigDecimal> castRates(Object value) {
        return (Map<String, BigDecimal>) value;
    }

    private String normalize(String base) {
        String code = base == null || base.isBlank() ? "USD" : base.trim().toUpperCase();
        if (!SUPPORTED.contains(code)) {
            throw new BusinessException(400, "Unsupported currency: " + code);
        }
        return code;
    }
}
