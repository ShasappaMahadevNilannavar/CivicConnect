package com.civicconnect.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadPath;
    private final List<String> allowedTypes;
    private final long maxSize;

    public FileStorageService(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.allowed-types}") String allowedTypes,
            @Value("${app.upload.max-size}") long maxSize) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedTypes = Arrays.asList(allowedTypes.split(","));
        this.maxSize = maxSize;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadPath);
            logger.info("Upload directory initialized at: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }
    }

    public String storeFile(MultipartFile file) {
        List<String> errors = validateImage(file);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid file: " + String.join(", ", errors));
        }

        String originalFileName = file.getOriginalFilename();
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String newFileName = UUID.randomUUID().toString() + extension;

        try {
            Path targetLocation = uploadPath.resolve(newFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Stored file: {} as {}", originalFileName, newFileName);
            return newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Could not store file " + originalFileName, e);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = uploadPath.resolve(fileName).normalize();
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                logger.info("Deleted file: {}", fileName);
            } else {
                logger.warn("File not found for deletion: {}", fileName);
            }
        } catch (IOException e) {
            logger.error("Could not delete file: {}", fileName, e);
        }
    }

    public Path getFilePath(String fileName) {
        return uploadPath.resolve(fileName).normalize();
    }

    public List<String> validateImage(MultipartFile file) {
        List<String> errors = new ArrayList<>();

        if (file == null || file.isEmpty()) {
            errors.add("File is empty");
            return errors;
        }

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.trim())) {
            errors.add("File type '" + contentType + "' is not allowed. Allowed types: " + String.join(", ", allowedTypes));
        }

        if (file.getSize() > maxSize) {
            errors.add("File size exceeds maximum allowed size of " + (maxSize / (1024 * 1024)) + " MB");
        }

        return errors;
    }
}
