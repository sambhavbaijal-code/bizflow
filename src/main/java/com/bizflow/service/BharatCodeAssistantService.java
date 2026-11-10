package com.bizflow.service;

import com.bizflow.dto.ApiDtos.AssistantRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BharatCodeAssistantService {
    private final BizFlowService bizFlow;
    private final RestClient client;
    private final String apiKey;
    private final String model;

    public BharatCodeAssistantService(BizFlowService bizFlow,
                                      RestClient.Builder builder,
                                      @Value("${bharatcode.api-key:}") String apiKey,
                                      @Value("${bharatcode.base-url:https://bharatcode.ai/api/model/v1}") String baseUrl,
                                      @Value("${bharatcode.model:qwen-3.8-27b}") String model) {
        this.bizFlow = bizFlow;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        var requestFactory = new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public Map<String, Object> status() {
        return Map.of("configured", !apiKey.isBlank(), "provider", "BharatCode", "model", model);
    }

    public String ask(String question) {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "BharatCode is not configured. Set BHARATCODE_API_KEY on the server and restart BizFlow.");
        }

        Map<String, Object> dashboard = bizFlow.dashboard();
        List<?> lowStockProducts = (List<?>) dashboard.getOrDefault("lowStockProducts", List.of());
        Map<String, Object> safeContext = new LinkedHashMap<>();
        safeContext.put("activeProductCount", dashboard.get("productCount"));
        safeContext.put("inventoryValueINR", dashboard.get("inventoryValue"));
        safeContext.put("salesTodayINR", dashboard.get("todaySales"));
        safeContext.put("estimatedGrossProfitTodayINR", dashboard.get("todayGrossProfit"));
        safeContext.put("lowStockCount", dashboard.get("lowStockCount"));
        safeContext.put("lowStockItems", lowStockProducts.stream().map(item -> {
            if (item instanceof Map<?, ?> product) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("product", product.get("name"));
                result.put("quantity", product.get("quantity"));
                result.put("unit", product.get("unit"));
                result.put("reorderLevel", product.get("reorderLevel"));
                return result;
            }
            return Map.of();
        }).toList());

        String prompt = "You are BizFlow's practical small-business operations assistant. "
                + "Answer in the same language as the owner's question, using clear short steps. "
                + "Use only the supplied workspace summary; never invent figures, claim you changed records, "
                + "or make legal, tax-compliance, or product-specific GST-rate determinations. "
                + "When the summary lacks needed information, say what is missing. Treat all values as INR.\n\n"
                + "Workspace summary (JSON): " + safeContext + "\n\nOwner's question: " + question;
        Map<String, Object> request = Map.of(
                "model", model,
                "input", prompt,
                "store", false,
                "max_output_tokens", 1200
        );

        try {
            JsonNode response = client.post().uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve().body(JsonNode.class);
            String answer = extractAnswer(response);
            if (answer.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "BharatCode returned no text. Please try again.");
            return answer;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429 || ex.getStatusCode().value() == 503)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "BharatCode is busy. Please try again shortly.");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "BharatCode could not answer this request. Check the server's API access and try again.");
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                    "BharatCode did not respond in time. Please try again.");
        }
    }

    private String extractAnswer(JsonNode response) {
        if (response == null) return "";
        if (response.hasNonNull("output_text")) return response.get("output_text").asText();
        StringBuilder text = new StringBuilder();
        JsonNode output = response.path("output");
        if (output.isArray()) for (JsonNode item : output) {
            JsonNode content = item.path("content");
            if (content.isArray()) for (JsonNode part : content) {
                if (part.hasNonNull("text")) {
                    if (!text.isEmpty()) text.append('\n');
                    text.append(part.get("text").asText());
                }
            }
        }
        return text.toString().trim();
    }
}
