package com.code.back_end.service;

import com.code.back_end.exception.InvalidFileException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService localService;

    @BeforeEach
    void setUp() {
        localService = new LocalFileStorageService(tempDir.toString());
    }

    @Test
    void testFallbackToLocalWhenUnconfigured() throws IOException {
        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "", "", "uploads", localService
        );

        assertFalse(service.isConfigured());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-id.jpg",
                "image/jpeg",
                "sample-image-content".getBytes()
        );

        String resultPath = service.storeFile(file);
        assertNotNull(resultPath);
        assertTrue(resultPath.toLowerCase().contains("test"));

        // Verify delete
        boolean deleted = service.deleteFile(resultPath);
        assertTrue(deleted);
    }

    @Test
    void testValidateFileRejectsDisallowedExtension() {
        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "", "", "uploads", localService
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "dummy".getBytes()
        );

        assertThrows(InvalidFileException.class, () -> service.validateFile(file));
    }

    @Test
    void testValidateFileAcceptsValidPdfAndImages() {
        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "", "", "uploads", localService
        );

        MockMultipartFile jpg = new MockMultipartFile("file", "doc.jpg", "image/jpeg", "image-bytes".getBytes());
        MockMultipartFile png = new MockMultipartFile("file", "doc.png", "image/png", "image-bytes".getBytes());
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", "pdf-bytes".getBytes());

        assertDoesNotThrow(() -> service.validateFile(jpg));
        assertDoesNotThrow(() -> service.validateFile(png));
        assertDoesNotThrow(() -> service.validateFile(pdf));
    }

    @Test
    void testIsConfiguredTrueWhenCredentialsProvided() {
        SupabaseFileStorageService service = new SupabaseFileStorageService(
                "https://test.supabase.co", "test-key", "uploads", localService
        );

        assertTrue(service.isConfigured());
    }
}
