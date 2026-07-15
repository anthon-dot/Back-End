package com.code.back_end.controller;

import com.code.back_end.dto.StallDTO;
import com.code.back_end.entity.Stall;
import com.code.back_end.service.StallService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    private final StallService service;

    public StallController(
            StallService service
    ) {
        this.service = service;
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

            String fileName =
                    System.currentTimeMillis()
                    + "_"
                    + file.getOriginalFilename();

            Path uploadPath =
                    Paths.get("uploads");

            if (!Files.exists(uploadPath)) {

                Files.createDirectories(uploadPath);
            }

            Files.copy(
                    file.getInputStream(),
                    uploadPath.resolve(fileName),
                    StandardCopyOption.REPLACE_EXISTING
            );

            return ResponseEntity.ok(
                    "/uploads/" + fileName
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body("Upload failed");
        }
    }
}