package com.roadvision.detection;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Calls Roboflow's hosted inference API directly — no self-hosted Python service needed.
 * Free tier: 15 credits/month (~15,000 calls), plenty for this project. Applies the same
 * severity-classification responsibility as the other {@link AIDetectionService}
 * implementations; Roboflow's model only returns raw class/confidence/box.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "roboflow")
@Slf4j
public class RoboflowAIDetectionServiceImpl implements AIDetectionService {

    private final RestClient roboflowRestClient;
    private final String apiKey;
    private final String modelId;
    private final double confidenceThreshold;

    public RoboflowAIDetectionServiceImpl(
            RestClient roboflowRestClient,
            @Value("${app.ai.roboflow.api-key}") String apiKey,
            @Value("${app.ai.roboflow.model-id}") String modelId,
            @Value("${app.ai.roboflow.confidence-threshold:0.25}") double confidenceThreshold
    ) {
        this.roboflowRestClient = roboflowRestClient;
        this.apiKey = apiKey;
        this.modelId = modelId;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    public DetectionResult detect(byte[] imageBytes) {
        RoboflowDetectionResponse response = callRoboflow(imageBytes);

        List<RoboflowPrediction> predictions = response != null ? response.predictions() : List.of();
        if (predictions.isEmpty() || response.image() == null) {
            return new DetectionResult(DamageType.POTHOLE, Severity.LOW, BigDecimal.ZERO, List.of());
        }

        int imageWidth = response.image().width();
        int imageHeight = response.image().height();

        RoboflowPrediction primary = predictions.stream()
                .max(Comparator.comparingDouble(RoboflowPrediction::confidence))
                .orElseThrow();

        DamageType damageType = mapDamageType(primary.className());
        BigDecimal confidence = BigDecimal.valueOf(primary.confidence() * 100).setScale(2, RoundingMode.HALF_UP);
        Severity severity = classifySeverity(primary, imageWidth, imageHeight);

        List<BoundingBox> boxes = predictions.stream()
                .map(p -> toNormalizedBox(p, imageWidth, imageHeight))
                .toList();

        return new DetectionResult(damageType, severity, confidence, boxes);
    }

    private RoboflowDetectionResponse callRoboflow(byte[] imageBytes) {
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String uri = UriComponentsBuilder.fromPath("/{modelId}")
                .queryParam("api_key", apiKey)
                .queryParam("confidence", confidenceThreshold)
                .buildAndExpand(modelId)
                .toUriString();

        try {
            return roboflowRestClient.post()
                    .uri(uri)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(base64Image)
                    .retrieve()
                    .body(RoboflowDetectionResponse.class);
        } catch (Exception ex) {
            log.error("Roboflow inference call failed, falling back to a low-confidence result", ex);
            return null;
        }
    }

    private BoundingBox toNormalizedBox(RoboflowPrediction p, int imageWidth, int imageHeight) {
        double normWidth = p.width() / imageWidth;
        double normHeight = p.height() / imageHeight;
        double normX = (p.x() - p.width() / 2.0) / imageWidth;
        double normY = (p.y() - p.height() / 2.0) / imageHeight;

        return new BoundingBox(
                round(normX), round(normY), round(normWidth), round(normHeight),
                p.className(), Math.round(p.confidence() * 10000.0) / 100.0
        );
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    private DamageType mapDamageType(String className) {
        String normalized = className.toLowerCase(Locale.ROOT);
        if (normalized.contains("crack")) {
            return DamageType.CRACK;
        }
        if (normalized.contains("surface") || normalized.contains("raveling") || normalized.contains("patch")) {
            return DamageType.SURFACE_DAMAGE;
        }
        return DamageType.POTHOLE;
    }

    private Severity classifySeverity(RoboflowPrediction primary, int imageWidth, int imageHeight) {
        double boxAreaFraction = (primary.width() * primary.height()) / ((double) imageWidth * imageHeight);
        double confidencePercent = primary.confidence() * 100;

        if (boxAreaFraction > 0.15 || confidencePercent > 85) {
            return Severity.HIGH;
        }
        if (boxAreaFraction > 0.06 || confidencePercent > 65) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }
}
