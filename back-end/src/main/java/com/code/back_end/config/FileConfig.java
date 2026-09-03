package com.code.back_end.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class FileConfig
        implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {
        String uploadDir = java.nio.file.Paths.get("uploads").toAbsolutePath().normalize().toString();
        if (!uploadDir.endsWith(java.io.File.separator)) {
            uploadDir += java.io.File.separator;
        }

        registry
                .addResourceHandler("/uploads/**", "/api/uploads/**")
                .addResourceLocations("file:" + uploadDir, "file:uploads/");
    }
}