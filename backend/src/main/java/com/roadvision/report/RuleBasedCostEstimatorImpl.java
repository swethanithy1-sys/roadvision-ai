package com.roadvision.report;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Default cost estimator: the deterministic base-cost x severity-multiplier formula from
 * {@link RepairEstimator}. Free, instant, no external dependency — this is the active
 * implementation unless {@code app.cost.provider=groq} is set.
 */
@Service
@ConditionalOnProperty(name = "app.cost.provider", havingValue = "rule", matchIfMissing = true)
@RequiredArgsConstructor
public class RuleBasedCostEstimatorImpl implements CostEstimator {

    private final RepairEstimator repairEstimator;

    @Override
    public BigDecimal estimateCost(CostEstimationContext context) {
        return repairEstimator.estimateCost(context.damageType(), context.severity());
    }
}
