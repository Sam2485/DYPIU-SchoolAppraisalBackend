package com.director_appraisal.director_appraisal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.local-path}")
    private String localUploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(localUploadPath);
        String uploadPath = uploadDir.toFile().getAbsolutePath();
        
        // Maps /uploads/** url to both file:/.../uploads/ and file:/.../uploads/users/
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/", "file:" + uploadPath + "/users/");
    }
}
