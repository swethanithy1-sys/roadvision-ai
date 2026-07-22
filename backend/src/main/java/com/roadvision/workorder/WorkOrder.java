package com.roadvision.workorder;

import com.roadvision.report.Report;
import com.roadvision.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkOrder {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, unique = true)
    private Report report;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "materials_required", nullable = false, columnDefinition = "jsonb")
    private List<String> materialsRequired;

    @Column(name = "estimated_labor_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal estimatedLaborHours;

    @Column(name = "estimated_duration_days", nullable = false, precision = 5, scale = 2)
    private BigDecimal estimatedDurationDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @CreatedDate
    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    public WorkOrder(Report report, List<String> materialsRequired, BigDecimal estimatedLaborHours, BigDecimal estimatedDurationDays, User generatedBy) {
        this.report = report;
        this.materialsRequired = materialsRequired;
        this.estimatedLaborHours = estimatedLaborHours;
        this.estimatedDurationDays = estimatedDurationDays;
        this.generatedBy = generatedBy;
    }
}
