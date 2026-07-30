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
        Path uploadDir = Paths.get(localUploadPath).toAbsolutePath().normalize();
        String uploadPath = uploadDir.toString().replace("\\", "/");
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        java.util.List<String> locations = new java.util.ArrayList<>();
        locations.add("file:" + uploadPath);
        locations.add("file:" + uploadPath + "users/");
        locations.add("file:/app/uploads-test/");
        locations.add("file:/app/uploads-test/users/");
        locations.add("file:/app/uploads/");
        locations.add("file:/app/uploads/users/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(locations.toArray(new String[0]));
    }
}
