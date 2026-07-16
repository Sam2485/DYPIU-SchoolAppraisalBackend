package com.director_appraisal.director_appraisal.config;

import com.director_appraisal.director_appraisal.util.UrlPostProcessor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UrlPostProcessorConfig {

    @Value("${app.gcp.enabled:false}")
    private boolean gcpEnabled;

    @Value("${app.gcp.bucket-name:schoolappraisal-attachments}")
    private String bucketName;

    @PostConstruct
    public void init() {
        UrlPostProcessor.init(gcpEnabled, bucketName);
    }
}
