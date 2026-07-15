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

        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}