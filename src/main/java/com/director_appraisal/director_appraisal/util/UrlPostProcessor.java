package com.director_appraisal.director_appraisal.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UrlPostProcessor {
    private static boolean gcpEnabled = false;
    private static String bucketName = "schoolappraisal-attachments";

    public static void init(boolean gcpEnabled, String bucketName) {
        UrlPostProcessor.gcpEnabled = gcpEnabled;
        if (bucketName != null && !bucketName.isBlank()) {
            UrlPostProcessor.bucketName = bucketName;
        }
        log.info("Initialized UrlPostProcessor with gcpEnabled={}, bucketName={}", UrlPostProcessor.gcpEnabled, UrlPostProcessor.bucketName);
    }

    public static String process(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        
        // If GCP is disabled (on VM), rewrite absolute GCS URLs to local relative paths
        // Example: https://storage.googleapis.com/schoolappraisal-attachments/users/... -> /uploads/users/...
        String pattern1 = "https://storage.googleapis.com/" + bucketName + "/";
        String res = json.replace(pattern1, "/uploads/");
        
        // Fallback replacements for other common bucket name patterns
        res = res.replace("https://storage.googleapis.com/schoolappraisal-attachments/", "/uploads/");
        res = res.replace("https://storage.googleapis.com/director-appraisal-attachments/", "/uploads/");
        
        return res;
    }
}
