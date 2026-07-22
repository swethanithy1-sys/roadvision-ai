package com.roadvision.detection;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.Severity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic stand-in for a real computer-vision model: the same image bytes always
 * produce the same result (seeded from a SHA-256 hash of the file), while different
 * photos plausibly vary in damage type, severity, and confidence.
 */
@Service
public class MockAIDetectionServiceImpl implements AIDetectionService {

    private static final DamageType[] DAMAGE_TYPES = DamageType.values();
    private static final Severity[] SEVERITIES = Severity.values();

    @Override
    public DetectionResult detect(byte[] imageBytes) {
        Random random = new Random(seedFrom(imageBytes));

        DamageType damageType = DAMAGE_TYPES[random.nextInt(DAMAGE_TYPES.length)];
        Severity severity = weightedSeverity(random);
        BigDecimal confidence = randomConfidence(random, severity);
        List<BoundingBox> boxes = generateBoundingBoxes(random, damageType, confidence);

        return new DetectionResult(damageType, severity, confidence, boxes);
    }

    private long seedFrom(byte[] imageBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(imageBytes);
            long seed = 0L;
            for (int i = 0; i < Long.BYTES; i++) {
                seed = (seed << 8) | (hash[i] & 0xFF);
            }
            return seed;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every JVM; this branch is unreachable.
            throw new IllegalStateException(e);
        }
    }

    private Severity weightedSeverity(Random random) {
        int roll = random.nextInt(100);
        if (roll < 40) return Severity.LOW;
        if (roll < 75) return Severity.MEDIUM;
        return Severity.HIGH;
    }

    private BigDecimal randomConfidence(Random random, Severity severity) {
        double base = switch (severity) {
            case LOW -> 60;
            case MEDIUM -> 72;
            case HIGH -> 82;
        };
        double confidence = base + random.nextDouble() * 16;
        return BigDecimal.valueOf(Math.min(confidence, 99.0)).setScale(2, RoundingMode.HALF_UP);
    }

    private List<BoundingBox> generateBoundingBoxes(Random random, DamageType damageType, BigDecimal overallConfidence) {
        List<BoundingBox> boxes = new ArrayList<>();
        int boxCount = 1 + random.nextInt(2);

        for (int i = 0; i < boxCount; i++) {
            double width = 0.15 + random.nextDouble() * 0.25;
            double height = 0.12 + random.nextDouble() * 0.22;
            double x = random.nextDouble() * (1 - width);
            double y = random.nextDouble() * (1 - height);
            double boxConfidence = overallConfidence.doubleValue() - random.nextDouble() * 5;

            boxes.add(new BoundingBox(
                    round(x), round(y), round(width), round(height),
                    damageType.name(),
                    Math.round(boxConfidence * 100.0) / 100.0
            ));
        }

        return boxes;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
