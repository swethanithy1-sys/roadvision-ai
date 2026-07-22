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

/**
 * Delegates repair cost estimation to an LLM via Groq's free, OpenAI-compatible chat
 * completions API (no Anthropic SDK involved — a different provider, plain REST, same
 * pattern as {@link com.roadvision.detection.RoboflowAIDetectionServiceImpl}). Falls back
 * to {@link RepairEstimator}'s rule-based formula on any failure — a report submission
 * should never fail just because the LLM call did.
 */
@Service
@ConditionalOnProperty(name = "app.cost.provider", havingValue = "groq")
@Slf4j
public class GroqCostEstimatorImpl implements CostEstimator {

    private static final String SYSTEM_PROMPT = """
            You are a road maintenance cost estimator for Indian municipal authorities.
            Given a detected road damage type, its severity, confidence, and location, \
            respond with ONLY a JSON object (no markdown, no prose) of the exact shape:
            {"estimated_cost_inr": <number>, "reasoning": "<one short sentence>"}
            The cost must be a realistic INR amount for repairing that damage in India, \
            reflecting typical municipal contractor rates. Higher severity and larger, \
            more confident detections should generally cost more to repair.""";

    private final RestClient groqRestClient;
    private final RepairEstimator repairEstimator;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GroqCostEstimatorImpl(
            RestClient groqRestClient,
            RepairEstimator repairEstimator,
            ObjectMapper objectMapper,
            @Value("${app.cost.groq.api-key}") String apiKey,
            @Value("${app.cost.groq.model}") String model
    ) {
        this.groqRestClient = groqRestClient;
        this.repairEstimator = repairEstimator;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public BigDecimal estimateCost(CostEstimationContext context) {
        try {
            String userPrompt = buildUserPrompt(context);

            GroqChatRequest request = new GroqChatRequest(
                    model,
                    List.of(
                            new GroqChatMessage("system", SYSTEM_PROMPT),
                            new GroqChatMessage("user", userPrompt)
                    ),
                    0.2,
                    GroqResponseFormat.jsonObject()
            );

            GroqChatResponse response = groqRestClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(GroqChatResponse.class);

            String content = response.choices().get(0).message().content();
            GroqCostEstimate estimate = objectMapper.readValue(content, GroqCostEstimate.class);

            return BigDecimal.valueOf(estimate.estimatedCostInr()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            log.error("Groq cost estimation failed, falling back to the rule-based estimator", ex);
            return repairEstimator.estimateCost(context.damageType(), context.severity());
        }
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
