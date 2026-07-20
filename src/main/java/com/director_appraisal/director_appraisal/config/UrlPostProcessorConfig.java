package com.director_appraisal.director_appraisal.config;

import com.director_appraisal.director_appraisal.util.UrlPostProcessor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UrlPostProcessorConfig {

    @PostConstruct
    public void init() {
        UrlPostProcessor.init(false, "schoolappraisal-attachments");
    }
}
