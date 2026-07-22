package com.roadvision.report;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.RepairPriority;
import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.constants.Severity;
import com.roadvision.detection.BoundingBox;
import com.roadvision.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "image_path", nullable = false, length = 500)
    private String imagePath;

    private Double latitude;

    private Double longitude;

    @Column(name = "address_text", length = 500)
    private String addressText;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false, length = 30)
    private DamageType damageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(name = "confidence_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bounding_boxes", nullable = false, columnDefinition = "jsonb")
    private List<BoundingBox> boundingBoxes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "repair_priority", nullable = false, length = 20)
    private RepairPriority repairPriority;

    @Column(name = "estimated_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.SUBMITTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    private User assignedTo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Report(
            User reporter,
            String imagePath,
            Double latitude,
            Double longitude,
            String addressText,
            String description,
            DamageType damageType,
            Severity severity,
            BigDecimal confidenceScore,
            List<BoundingBox> boundingBoxes,
            RepairPriority repairPriority,
            BigDecimal estimatedCost
    ) {
        this.reporter = reporter;
        this.imagePath = imagePath;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressText = addressText;
        this.description = description;
        this.damageType = damageType;
        this.severity = severity;
        this.confidenceScore = confidenceScore;
        this.boundingBoxes = boundingBoxes;
        this.repairPriority = repairPriority;
        this.estimatedCost = estimatedCost;
        this.status = ReportStatus.SUBMITTED;
    }
}
