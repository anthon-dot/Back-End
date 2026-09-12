package com.code.back_end.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@RestController
public class FileDownloadController {

    private static final Logger log = LoggerFactory.getLogger(FileDownloadController.class);

    private final Path uploadLocation;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucketName;
    private final HttpClient httpClient;

    public FileDownloadController(
            @Value("${app.upload.dir:uploads}") String uploadDir,
            @Value("${supabase.url:}") String supabaseUrl,
            @Value("${supabase.service-role-key:${supabase.key:}}") String supabaseKey,
            @Value("${supabase.storage.bucket:uploads}") String bucketName
    ) {
        this.uploadLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim().replaceAll("/+$", "") : "";
        this.supabaseKey = supabaseKey != null ? supabaseKey.trim() : "";
        this.bucketName = (bucketName != null && !bucketName.isBlank()) ? bucketName.trim() : "uploads";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @GetMapping({"/uploads/{fileName:.+}", "/api/uploads/{fileName:.+}"})
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            // Prevent directory traversal
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                return ResponseEntity.badRequest().build();
            }

            // 1. Check local file system
            Path filePath = this.uploadLocation.resolve(fileName).normalize();
            if (filePath.startsWith(this.uploadLocation) && Files.exists(filePath)) {
                Resource resource = new UrlResource(filePath.toUri());
                if (resource.exists() && resource.isReadable()) {
                    String contentType = resolveContentType(fileName, filePath);
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                }
            }

            // 2. If not found locally, fetch from Supabase Storage if configured
            if (!supabaseUrl.isBlank()) {
                String supabaseEndpoint = String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, fileName);

                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(supabaseEndpoint))
                        .timeout(Duration.ofSeconds(15))
                        .GET();

                if (!supabaseKey.isBlank()) {
                    reqBuilder.header("Authorization", "Bearer " + supabaseKey);
                    reqBuilder.header("apikey", supabaseKey);
                }

                try {
                    HttpResponse<byte[]> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null) {
                        String contentType = response.headers().firstValue("Content-Type").orElse(null);
                        if (contentType == null || contentType.isBlank() || contentType.equals("application/octet-stream")) {
                            contentType = resolveContentType(fileName, null);
                        }

                        ByteArrayResource resource = new ByteArrayResource(response.body());
                        return ResponseEntity.ok()
                                .contentType(MediaType.parseMediaType(contentType))
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                                .body(resource);
                    } else {
                        log.debug("File '{}' not found in Supabase Storage. Status: {}", fileName, response.statusCode());
                    }
                } catch (Exception e) {
                    log.error("Failed to retrieve file '{}' from Supabase Storage: {}", fileName, e.getMessage());
                }
            }

            return ResponseEntity.notFound().build();
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String resolveContentType(String fileName, Path filePath) {
        String contentType = null;
        if (filePath != null) {
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ignored) {}
        }

        if (contentType == null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".png")) {
                contentType = "image/png";
            } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lower.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lower.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else {
                contentType = "application/octet-stream";
            }
        }
        return contentType;
    }
}
