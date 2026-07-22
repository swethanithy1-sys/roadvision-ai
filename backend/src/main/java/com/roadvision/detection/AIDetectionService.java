package com.roadvision.detection;

/**
 * Computer-vision detection abstraction. {@link MockAIDetectionServiceImpl} is the only
 * implementation today; a future implementation can call out to a Python/YOLOv8/OpenCV
 * service over REST and be swapped in via Spring's dependency injection with zero changes
 * to callers (ReportService) or the frontend contract.
 */
public interface AIDetectionService {
    DetectionResult detect(byte[] imageBytes);
}
