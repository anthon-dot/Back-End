package com.code.back_end.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileDownloadControllerTest {

    @TempDir
    Path tempDir;

    private FileDownloadController controller;

    @BeforeEach
    void setUp() {
        controller = new FileDownloadController(tempDir.toString(), "", "", "uploads");
    }

    @Test
    void testServeLocalFileSuccessfully() throws IOException {
        String fileName = "test-doc.jpg";
        Path testFile = tempDir.resolve(fileName);
        Files.write(testFile, "sample-image-data".getBytes());

        ResponseEntity<Resource> response = controller.serveFile(fileName);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("image/jpeg", response.getHeaders().getContentType().toString());
    }

    @Test
    void testServeFileRejectsPathTraversal() {
        ResponseEntity<Resource> responseDotDot = controller.serveFile("../secret.txt");
        assertEquals(HttpStatus.BAD_REQUEST, responseDotDot.getStatusCode());

        ResponseEntity<Resource> responseSlash = controller.serveFile("sub/secret.txt");
        assertEquals(HttpStatus.BAD_REQUEST, responseSlash.getStatusCode());

        ResponseEntity<Resource> responseBackslash = controller.serveFile("sub\\secret.txt");
        assertEquals(HttpStatus.BAD_REQUEST, responseBackslash.getStatusCode());
    }

    @Test
    void testServeFileReturnsNotFoundWhenNonExistent() {
        ResponseEntity<Resource> response = controller.serveFile("missing-file.jpg");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
