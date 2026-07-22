package com.roadvision.report;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.Severity;

import java.math.BigDecimal;

/** Everything a {@link CostEstimator} implementation might reasonably reason from. */
public record CostEstimationContext(
        DamageType damageType,
        Severity severity,
        BigDecimal confidenceScore,
        String addressText,
        String description
) {
}
