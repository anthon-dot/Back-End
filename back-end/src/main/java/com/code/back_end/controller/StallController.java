package com.code.back_end.controller;

import com.code.back_end.dto.StallDTO;
import com.code.back_end.entity.Stall;
import com.code.back_end.service.FileStorageService;
import com.code.back_end.service.StallService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    private final StallService service;
    private final FileStorageService fileStorageService;

    public StallController(
            StallService service,
            FileStorageService fileStorageService
    ) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<StallDTO> getAll() {

        return service.findAll()
                .stream()
                .map(StallDTO::new)
                .collect(Collectors.toList());
    }

    @PostMapping
    public Stall create(
            @RequestBody Stall stall
    ) {
        return service.save(stall);
    }

    @PutMapping("/{id}")
    public Stall update(
            @PathVariable Long id,
            @RequestBody Stall stall
    ) {

        stall.setId(id);

        return service.save(stall);
    }

    @DeleteMapping("/{id}")
    public void delete(
            @PathVariable Long id
    ) {
        service.deleteById(id);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file")
            MultipartFile file
    ) {

        try {
            String storedPath = fileStorageService.storeFile(file);
            if (storedPath.startsWith("http://") || storedPath.startsWith("https://")) {
                return ResponseEntity.ok(storedPath);
            }

            int lastSlash = Math.max(storedPath.lastIndexOf('/'), storedPath.lastIndexOf('\\'));
            String fileName = lastSlash >= 0 ? storedPath.substring(lastSlash + 1) : storedPath;
            return ResponseEntity.ok("/uploads/" + fileName);

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body("Upload failed: " + e.getMessage());
        }
    }
}