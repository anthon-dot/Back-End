package com.code.back_end.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {
    String storeFile(MultipartFile file) throws IOException;
    void validateFile(MultipartFile file);
    boolean deleteFile(String filePath);
}
