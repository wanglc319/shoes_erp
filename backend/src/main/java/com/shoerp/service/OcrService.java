package com.shoerp.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shoerp.common.BusinessException;

/**
 * 图片报价单识别：调用 OpenAI 兼容的视觉接口。
 * 通过 erp.ocr.* 配置启用，未配置时返回明确提示。
 */
@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    private static final String PROMPT = """
            你是外贸鞋类报价单识别助手。请从图片中提取报价明细，只输出 JSON 数组，不要任何解释或 markdown。
            数组每项字段：
            {"styleNo":"款号","description":"品名","material":"材料","color":"颜色","sizeRange":"尺码",
             "quantity":数量数字,"unitPrice":单价数字,"leadDate":"交期 yyyy-MM-dd 或空"}
            识别不到的字段填 null。款号和单价尽量准确。
            """;

    private final ObjectMapper objectMapper;

    @Value("${erp.ocr.enabled:false}")
    private boolean enabled;

    @Value("${erp.ocr.base-url:}")
    private String baseUrl;

    @Value("${erp.ocr.api-key:}")
    private String apiKey;

    @Value("${erp.ocr.model:}")
    private String model;

    public OcrService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return enabled
                && baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank();
    }

    /**
     * 返回模型输出的原始文本，由调用方解析。
     */
    public String recognize(byte[] imageBytes, String contentType) {
        if (!isConfigured()) {
            throw new BusinessException(503,
                    "Image OCR is not configured. Set erp.ocr.enabled/base-url/api-key/model first.");
        }
        try {
            String mime = contentType == null || contentType.isBlank() ? "image/png" : contentType;
            String dataUri = "data:" + mime + ";base64,"
                    + Base64.getEncoder().encodeToString(imageBytes);
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0,
                    "messages", List.of(Map.of(
                            "role", "user",
                            "content", List.of(
                                    Map.of("type", "text", "text", PROMPT),
                                    Map.of("type", "image_url", "image_url", Map.of("url", dataUri))))));

            String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build()
                    .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                log.warn("OCR request failed with status {}: {}", response.statusCode(), response.body());
                throw new BusinessException(502, "OCR service returned " + response.statusCode());
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new BusinessException(502, "OCR service returned empty result");
            }
            return content.asText();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("OCR call failed: {}", ex.getMessage());
            throw new BusinessException(502, "OCR request failed: " + ex.getMessage());
        }
    }
}
