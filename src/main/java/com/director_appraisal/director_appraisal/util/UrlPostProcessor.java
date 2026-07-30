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
        
        // Rewrite any Google Storage URL (regardless of bucket name) to local /uploads/ path
        String res = json.replaceAll("https://storage\\.googleapis\\.com/[^/\"]+/", "/uploads/");
        
        // Ensure any relative users/ attachment path starts with /uploads/
        res = res.replaceAll("\"(?:/)?users/", "\"/uploads/users/");
        
        return res;
    }
}
