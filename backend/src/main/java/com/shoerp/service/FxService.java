package com.shoerp.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.shoerp.common.BusinessException;

@Service
public class FxService {

    private static final List<String> SUPPORTED = List.of(
            "USD", "EUR", "CNY", "GBP", "AUD", "CAD", "JPY", "HKD", "SGD", "KRW", "INR", "CHF");

    private static final long CACHE_TTL_MILLIS = 10 * 60 * 1000L;

    private final RestClient restClient = RestClient.create("https://open.er-api.com");

    private final Map<String, CachedRates> cache = new ConcurrentHashMap<>();

    private record CachedRates(Map<String, BigDecimal> rates, String updatedAt, long fetchedAt) {
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> rates(String base) {
        String code = normalize(base);
        CachedRates cached = cache.get(code);
        boolean stale = true;
        String notice = null;
        if (cached == null || System.currentTimeMillis() - cached.fetchedAt() > CACHE_TTL_MILLIS) {
            try {
                Map<String, Object> body = restClient.get()
                        .uri("/v6/latest/{base}", code)
                        .retrieve()
                        .body(Map.class);
                Map<String, Object> raw = body == null ? null : (Map<String, Object>) body.get("rates");
                Map<String, BigDecimal> parsed = new LinkedHashMap<>();
                if (raw != null) {
                    for (String target : SUPPORTED) {
                        Object value = raw.get(target);
                        if (value != null) {
                            parsed.put(target, new BigDecimal(value.toString()));
                        }
                    }
                }
                if (!parsed.isEmpty()) {
                    String updatedAt = body.get("time_last_update_utc") == null
                            ? Instant.now().toString() : body.get("time_last_update_utc").toString();
                    cached = new CachedRates(parsed, updatedAt, System.currentTimeMillis());
                    cache.put(code, cached);
                    stale = false;
                }
            } catch (Exception ex) {
                if (cached == null) {
                    throw new BusinessException(503, "Failed to fetch exchange rates");
                }
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
        return result;
    }

    public Map<String, Object> convert(String from, String to, BigDecimal amount) {
        String source = normalize(from);
        String target = normalize(to);
        BigDecimal value = amount == null ? BigDecimal.ONE : amount;
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
