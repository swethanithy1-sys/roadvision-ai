package com.roadvision.report;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.RepairPriority;
import com.roadvision.common.constants.Severity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Derives repair priority and an approximate repair cost from AI detection output.
 * Kept separate from {@link com.roadvision.detection.AIDetectionService} because this is
 * business/domain logic (pricing, prioritization policy), not computer vision — it stays
 * stable even when the detection backend is swapped for a real model.
 */
@Component
public class RepairEstimator {

    private static final BigDecimal BASE_COST_POTHOLE = BigDecimal.valueOf(1500);
    private static final BigDecimal BASE_COST_CRACK = BigDecimal.valueOf(600);
    private static final BigDecimal BASE_COST_SURFACE_DAMAGE = BigDecimal.valueOf(2200);

    public RepairPriority estimatePriority(Severity severity) {
        return switch (severity) {
            case LOW -> RepairPriority.LOW;
            case MEDIUM -> RepairPriority.MEDIUM;
            case HIGH -> RepairPriority.URGENT;
        };
    }

    public BigDecimal estimateCost(DamageType damageType, Severity severity) {
        BigDecimal base = switch (damageType) {
            case POTHOLE -> BASE_COST_POTHOLE;
            case CRACK -> BASE_COST_CRACK;
            case SURFACE_DAMAGE -> BASE_COST_SURFACE_DAMAGE;
        };

        BigDecimal severityMultiplier = switch (severity) {
            case LOW -> BigDecimal.valueOf(1.0);
            case MEDIUM -> BigDecimal.valueOf(1.6);
            case HIGH -> BigDecimal.valueOf(2.4);
        };

        return base.multiply(severityMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
