package com.roadvision.detection;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Calls out to the ai-service/ FastAPI+YOLOv8 microservice for raw detections, then applies
 * the same severity-classification responsibility {@link MockAIDetectionServiceImpl} has —
 * the Python service intentionally only does computer vision, not business rules.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "real")
@Slf4j
public class RealAIDetectionServiceImpl implements AIDetectionService {

    private final RestClient aiServiceRestClient;

    public RealAIDetectionServiceImpl(RestClient aiServiceRestClient) {
        this.aiServiceRestClient = aiServiceRestClient;
    }

    @Override
    public DetectionResult detect(byte[] imageBytes) {
        RawDetectionResponse response = callDetectionService(imageBytes);
        List<RawDetection> detections = response != null ? response.detections() : List.of();

        if (detections.isEmpty()) {
            return new DetectionResult(DamageType.POTHOLE, Severity.LOW, BigDecimal.ZERO, List.of());
        }

        RawDetection primary = detections.stream()
                .max(Comparator.comparingDouble(RawDetection::confidence))
                .orElseThrow();

        DamageType damageType = mapDamageType(primary.className());
        BigDecimal confidence = BigDecimal.valueOf(primary.confidence()).setScale(2, RoundingMode.HALF_UP);
        Severity severity = classifySeverity(primary);

        List<BoundingBox> boxes = detections.stream()
                .map(d -> new BoundingBox(d.x(), d.y(), d.width(), d.height(), d.className(), d.confidence()))
                .toList();

        return new DetectionResult(damageType, severity, confidence, boxes);
    }

    private RawDetectionResponse callDetectionService(byte[] imageBytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return "upload.jpg";
            }
        });

        try {
            return aiServiceRestClient.post()
                    .uri("/detect")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA_VALUE)
                    .body(body)
                    .retrieve()
                    .body(RawDetectionResponse.class);
        } catch (Exception ex) {
            log.error("AI detection service call failed, falling back to a low-confidence result", ex);
            return null;
        }
    }

    /**
     * Maps the trained model's raw class names to our fixed enum. Adjust this if you swap in a
     * model whose class list differs (e.g. "alligator_crack", "longitudinal_crack" both mapping
     * to CRACK, or an explicit "surface_damage"/"raveling" class).
     */
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

    /**
     * The model only outputs class + confidence + box; severity is our own policy layered on
     * top (larger/more-confident detections read as more urgent), same idea as
     * MockAIDetectionServiceImpl's weighted random but driven by real detection signal here.
     */
    private Severity classifySeverity(RawDetection primary) {
        double boxArea = primary.width() * primary.height();
        if (boxArea > 0.15 || primary.confidence() > 85) {
            return Severity.HIGH;
        }
        if (boxArea > 0.06 || primary.confidence() > 65) {
            return Severity.MEDIUM;
        }
        return Severity.LOW;
    }
}
