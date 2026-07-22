package com.roadvision.storage;

import com.roadvision.common.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private final Path uploadRoot;

    public LocalFileStorageServiceImpl(@Value("${app.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] content, String originalFilename, String subDirectory) {
        if (content == null || content.length == 0) {
            throw new BadRequestException("An image file is required");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (extension != null ? "." + extension : "");
        String relativePath = subDirectory + "/" + storedFilename;

        try {
            Path targetDir = uploadRoot.resolve(subDirectory).normalize();
            Files.createDirectories(targetDir);

            Path targetFile = uploadRoot.resolve(relativePath).normalize();
            if (!targetFile.startsWith(uploadRoot)) {
                throw new BadRequestException("Invalid file destination");
            }

            Files.write(targetFile, content);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        return relativePath;
    }
}
