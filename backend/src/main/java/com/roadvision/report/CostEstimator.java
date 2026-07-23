package com.roadvision.report;

import java.math.BigDecimal;

/**
 * Estimates repair cost from AI detection output. Kept separate from
 * {@link com.roadvision.detection.AIDetectionService} for the same reason
 * {@link RepairEstimator} is — pricing is business/domain logic, not computer vision.
 * {@link RuleBasedCostEstimatorImpl} (default) is a deterministic lookup formula;
 * {@link GeminiCostEstimatorImpl} (opt-in) delegates the estimate to an LLM instead.
 */
public interface CostEstimator {
    BigDecimal estimateCost(CostEstimationContext context);
}
