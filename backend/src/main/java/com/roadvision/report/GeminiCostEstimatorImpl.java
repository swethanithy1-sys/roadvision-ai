package com.roadvision.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Delegates repair cost estimation to an LLM via Google's free Gemini Interactions API. Falls
 * back to {@link RepairEstimator}'s rule-based formula on any failure — a report submission
 * should never fail just because the LLM call did.
 */
@Service
@ConditionalOnProperty(name = "app.cost.provider", havingValue = "gemini")
@Slf4j
public class GeminiCostEstimatorImpl implements CostEstimator {

    private static final String SYSTEM_INSTRUCTION = """
            You are a road maintenance cost estimator for Indian municipal authorities.
            Given a detected road damage type, its severity, confidence, and location, \
            respond with a realistic INR repair cost estimate and a one-sentence reason.
            The cost must reflect typical Indian municipal contractor rates. Higher severity \
            and larger, more confident detections should generally cost more to repair.""";

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "estimated_cost_inr", Map.of(
                            "type", "number",
                            "description", "Estimated repair cost in Indian Rupees"
                    ),
                    "reasoning", Map.of(
                            "type", "string",
                            "description", "One short sentence explaining the estimate"
                    )
            ),
            "required", List.of("estimated_cost_inr", "reasoning")
    );

    private final RestClient geminiRestClient;
    private final RepairEstimator repairEstimator;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiCostEstimatorImpl(
            RestClient geminiRestClient,
            RepairEstimator repairEstimator,
            ObjectMapper objectMapper,
            @Value("${app.cost.gemini.api-key}") String apiKey,
            @Value("${app.cost.gemini.model}") String model
    ) {
        this.geminiRestClient = geminiRestClient;
        this.repairEstimator = repairEstimator;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public BigDecimal estimateCost(CostEstimationContext context) {
        try {
            GeminiInteractionRequest request = new GeminiInteractionRequest(
                    model,
                    SYSTEM_INSTRUCTION,
                    buildUserPrompt(context),
                    GeminiResponseFormat.jsonSchema(RESPONSE_SCHEMA)
            );

            GeminiInteractionResponse response = geminiRestClient.post()
                    .uri("/interactions")
                    .header("x-goog-api-key", apiKey)
                    .body(request)
                    .retrieve()
                    .body(GeminiInteractionResponse.class);

            String content = extractModelOutputText(response);
            GeminiCostEstimate estimate = objectMapper.readValue(content, GeminiCostEstimate.class);

            return BigDecimal.valueOf(estimate.estimatedCostInr()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            log.error("Gemini cost estimation failed, falling back to the rule-based estimator", ex);
            return repairEstimator.estimateCost(context.damageType(), context.severity());
        }
    }

    private String extractModelOutputText(GeminiInteractionResponse response) {
        return response.steps().stream()
                .filter(step -> "model_output".equals(step.type()))
                .flatMap(step -> step.content() == null ? Stream.empty() : step.content().stream())
                .filter(part -> "text".equals(part.type()))
                .map(GeminiInteractionResponse.GeminiContentPart::text)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No model_output text part found in Gemini response"));
    }

    private String buildUserPrompt(CostEstimationContext context) {
        return """
                Damage type: %s
                Severity: %s
                AI detection confidence: %s%%
                Location: %s
                Description: %s""".formatted(
                context.damageType(),
                context.severity(),
                context.confidenceScore(),
                context.addressText() != null ? context.addressText() : "not specified",
                context.description() != null ? context.description() : "not provided"
        );
    }
}
