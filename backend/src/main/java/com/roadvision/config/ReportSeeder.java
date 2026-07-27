package com.roadvision.config;

import com.roadvision.common.constants.DamageType;
import com.roadvision.common.constants.ReportStatus;
import com.roadvision.common.constants.Severity;
import com.roadvision.detection.BoundingBox;
import com.roadvision.report.RepairEstimator;
import com.roadvision.report.Report;
import com.roadvision.report.ReportRepository;
import com.roadvision.report.ReportStatusHistory;
import com.roadvision.report.ReportStatusHistoryRepository;
import com.roadvision.storage.FileStorageService;
import com.roadvision.user.User;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;

/**
 * Seeds sample reports spanning severities, statuses, and months so the analytics
 * dashboard, hazard map, and citizen dashboard aren't empty on first run. Runs after
 * {@link DataSeeder} (which provisions the demo users this seeder assigns reports to).
 * Placeholder photos are generated on the fly (colored by severity) rather than
 * committing binary fixtures to the repo.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class ReportSeeder implements CommandLineRunner {

    private static final String UPLOAD_SUBDIRECTORY = "reports";

    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReportStatusHistoryRepository statusHistoryRepository;
    private final FileStorageService fileStorageService;
    private final RepairEstimator repairEstimator;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    private record SeedReport(
            int daysAgo, String addressText, double lat, double lng,
            DamageType damageType, Severity severity, ReportStatus status, String reporterEmail
    ) {
    }

    private static final List<SeedReport> SEED_DATA = List.of(
            new SeedReport(140, "MG Road, Bengaluru", 12.9758, 77.6045, DamageType.POTHOLE, Severity.HIGH, ReportStatus.RESOLVED, "citizen1@roadvision.ai"),
            new SeedReport(115, "Outer Ring Road, Marathahalli", 12.9569, 77.7011, DamageType.CRACK, Severity.MEDIUM, ReportStatus.RESOLVED, "citizen2@roadvision.ai"),
            new SeedReport(95, "MG Road, Bengaluru", 12.9752, 77.6050, DamageType.SURFACE_DAMAGE, Severity.LOW, ReportStatus.RESOLVED, "citizen1@roadvision.ai"),
            new SeedReport(70, "Hosur Road, Electronic City", 12.8452, 77.6602, DamageType.POTHOLE, Severity.HIGH, ReportStatus.IN_PROGRESS, "citizen2@roadvision.ai"),
            new SeedReport(48, "Sarjapur Road, Bellandur", 12.9257, 77.6812, DamageType.POTHOLE, Severity.MEDIUM, ReportStatus.ASSIGNED, "citizen1@roadvision.ai"),
            new SeedReport(30, "Outer Ring Road, Marathahalli", 12.9581, 77.7002, DamageType.CRACK, Severity.LOW, ReportStatus.UNDER_REVIEW, "citizen2@roadvision.ai"),
            new SeedReport(18, "Hosur Road, Electronic City", 12.8460, 77.6588, DamageType.SURFACE_DAMAGE, Severity.HIGH, ReportStatus.UNDER_REVIEW, "citizen1@roadvision.ai"),
            new SeedReport(6, "Bannerghatta Road, JP Nagar", 12.9081, 77.5983, DamageType.POTHOLE, Severity.HIGH, ReportStatus.SUBMITTED, "citizen2@roadvision.ai"),
            new SeedReport(1, "Sarjapur Road, Bellandur", 12.9265, 77.6799, DamageType.CRACK, Severity.MEDIUM, ReportStatus.SUBMITTED, "citizen1@roadvision.ai")
    );

    @Override
    public void run(String... args) {
        if (!seedEnabled || reportRepository.count() > 0) {
            return;
        }

        for (SeedReport seed : SEED_DATA) {
            seedOne(seed);
        }

        log.info("Seeded {} sample reports across severities, statuses, and months", SEED_DATA.size());
    }

    private void seedOne(SeedReport seed) {
        User reporter = userRepository.findByEmail(seed.reporterEmail())
                .orElseThrow(() -> new IllegalStateException("Seed user not found: " + seed.reporterEmail()));

        BigDecimal confidence = confidenceFor(seed.severity());
        List<BoundingBox> boxes = List.of(new BoundingBox(0.28, 0.22, 0.32, 0.28, seed.damageType().name(), confidence.doubleValue() - 2));
        String imagePath = storePlaceholderImage(seed.severity(), seed.damageType());

        Report report = new Report(
                reporter,
                imagePath,
                seed.lat(),
                seed.lng(),
                seed.addressText(),
                "Seeded sample report for demo purposes.",
                seed.damageType(),
                seed.severity(),
                confidence,
                boxes,
                repairEstimator.estimatePriority(seed.severity()),
                repairEstimator.estimateCost(seed.damageType(), seed.severity())
        );
        report = reportRepository.save(report);

        Instant submittedAt = Instant.now().minus(seed.daysAgo(), ChronoUnit.DAYS);
        ReportStatusHistory submitted = statusHistoryRepository.save(
                new ReportStatusHistory(report, ReportStatus.SUBMITTED, "Report submitted by citizen", reporter)
        );
        backdateHistory(submitted.getId(), submittedAt);

        Instant lastChangeAt = submittedAt;
        if (seed.status() != ReportStatus.SUBMITTED) {
            lastChangeAt = submittedAt.plus(2, ChronoUnit.DAYS);
            ReportStatusHistory reviewed = statusHistoryRepository.save(
                    new ReportStatusHistory(report, ReportStatus.UNDER_REVIEW, "Report reviewed by maintenance team", null)
            );
            backdateHistory(reviewed.getId(), lastChangeAt);
        }
        if (seed.status() == ReportStatus.ASSIGNED || seed.status() == ReportStatus.IN_PROGRESS || seed.status() == ReportStatus.RESOLVED) {
            lastChangeAt = lastChangeAt.plus(2, ChronoUnit.DAYS);
            ReportStatusHistory assigned = statusHistoryRepository.save(
                    new ReportStatusHistory(report, ReportStatus.ASSIGNED, "Assigned to a repair crew", null)
            );
            backdateHistory(assigned.getId(), lastChangeAt);
        }
        if (seed.status() == ReportStatus.IN_PROGRESS || seed.status() == ReportStatus.RESOLVED) {
            lastChangeAt = lastChangeAt.plus(3, ChronoUnit.DAYS);
            ReportStatusHistory inProgress = statusHistoryRepository.save(
                    new ReportStatusHistory(report, ReportStatus.IN_PROGRESS, "Repair work started", null)
            );
            backdateHistory(inProgress.getId(), lastChangeAt);
        }
        if (seed.status() == ReportStatus.RESOLVED) {
            lastChangeAt = lastChangeAt.plus(4, ChronoUnit.DAYS);
            ReportStatusHistory resolved = statusHistoryRepository.save(
                    new ReportStatusHistory(report, ReportStatus.RESOLVED, "Repair completed and verified", null)
            );
            backdateHistory(resolved.getId(), lastChangeAt);
        }

        report.setStatus(seed.status());
        reportRepository.save(report);

        backdateReport(report.getId(), submittedAt, lastChangeAt);
    }

    private BigDecimal confidenceFor(Severity severity) {
        return switch (severity) {
            case LOW -> BigDecimal.valueOf(68.5);
            case MEDIUM -> BigDecimal.valueOf(79.2);
            case HIGH -> BigDecimal.valueOf(89.7);
        };
    }

    private void backdateReport(UUID reportId, Instant createdAt, Instant updatedAt) {
        jdbcTemplate.update(
                "UPDATE reports SET created_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(createdAt), Timestamp.from(updatedAt), reportId
        );
    }

    private void backdateHistory(UUID historyId, Instant changedAt) {
        jdbcTemplate.update(
                "UPDATE report_status_history SET changed_at = ? WHERE id = ?",
                Timestamp.from(changedAt), historyId
        );
    }

    private String storePlaceholderImage(Severity severity, DamageType damageType) {
        Color color = switch (severity) {
            case LOW -> new Color(16, 185, 129);
            case MEDIUM -> new Color(245, 158, 11);
            case HIGH -> new Color(239, 68, 68);
        };

        BufferedImage image = new BufferedImage(480, 320, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(30, 41, 59));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.setColor(color);
        g.fillRoundRect(40, 40, image.getWidth() - 80, image.getHeight() - 80, 24, 24);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(damageType.name().replace('_', ' '), 60, image.getHeight() / 2 - 10);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.drawString("Sample seed image — " + severity.name() + " severity", 60, image.getHeight() / 2 + 20);
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return fileStorageService.store(out.toByteArray(), "seed.jpg", UPLOAD_SUBDIRECTORY);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate placeholder seed image", e);
        }
    }
}
