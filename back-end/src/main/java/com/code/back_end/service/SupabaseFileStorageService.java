package com.code.back_end.service;

import com.code.back_end.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Primary
public class SupabaseFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseFileStorageService.class);

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10 MB

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "webp", "pdf"
    );

    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucketName;
    private final LocalFileStorageService localFallback;
    private final HttpClient httpClient;

    public SupabaseFileStorageService(
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:${supabase.key:}}") String supabaseKey,
            @Value("${supabase.storage.bucket:uploads}") String bucketName,
            LocalFileStorageService localFallback
    ) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim().replaceAll("/+$", "") : "";
        this.supabaseKey = supabaseKey != null ? supabaseKey.trim() : "";
        this.bucketName = (bucketName != null && !bucketName.isBlank()) ? bucketName.trim() : "uploads";
        this.localFallback = localFallback;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        if (isConfigured()) {
            log.info("Supabase Storage initialized for bucket '{}' at {}", this.bucketName, this.supabaseUrl);
        } else {
            log.warn("Supabase Storage not fully configured (missing supabase.url or supabase.service-role-key). Falling back to local storage.");
        }
    }

    public boolean isConfigured() {
        return !supabaseUrl.isBlank() && !supabaseKey.isBlank();
    }

    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file is empty or missing");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidFileException("File size exceeds the maximum limit of 10MB");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("File name is invalid or missing");
        }

        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new InvalidFileException("File name contains illegal path traversal characters");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new InvalidFileException("Unsupported file extension: ." + extension + ". Allowed: " + ALLOWED_EXTENSIONS);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Unsupported MIME type: " + contentType + ". Allowed: " + ALLOWED_MIME_TYPES);
        }
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        validateFile(file);

        if (!isConfigured()) {
            log.info("Supabase is not configured, using local file storage");
            return localFallback.storeFile(file);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String baseName = originalFilename.replaceFirst("[.][^.]+$", "").replaceAll("[^a-zA-Z0-9_-]", "_");
        String uniqueFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + baseName + "." + extension;

        String uploadEndpoint = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, uniqueFileName);

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadEndpoint))
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .header("Content-Type", contentType)
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String publicUrl = String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, uniqueFileName);
                log.info("Successfully uploaded file to Supabase: {}", publicUrl);
                return publicUrl;
            } else {
                log.error("Failed to upload file to Supabase. Status: {}, Body: {}. Falling back to local storage.",
                        response.statusCode(), response.body());
                return localFallback.storeFile(file);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Supabase upload interrupted: {}. Falling back to local storage.", e.getMessage());
            return localFallback.storeFile(file);
        } catch (Exception e) {
            log.error("Error uploading to Supabase: {}. Falling back to local storage.", e.getMessage(), e);
            return localFallback.storeFile(file);
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }

        if (!isConfigured() || !filePath.startsWith("http")) {
            return localFallback.deleteFile(filePath);
        }

        try {
            int lastSlash = filePath.lastIndexOf('/');
            String fileName = lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;

            String deleteEndpoint = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, fileName);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteEndpoint))
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("apikey", supabaseKey)
                    .DELETE()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            log.error("Error deleting file from Supabase: {}", e.getMessage(), e);
            return false;
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDotIndex + 1);
    }
}
