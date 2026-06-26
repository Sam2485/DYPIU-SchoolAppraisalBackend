package com.director_appraisal.director_appraisal.util;

import java.util.Locale;
import java.util.Set;

public class SchoolUtils {

    private static final Set<String> VALID_SCHOOLS_LOWERCASE = Set.of(
            // Abbreviations
            "socsea",
            "socm",
            "sobb",
            "somcs",
            "somcs",
            "sod",
            "soaa",
            "soce",
            "soemr",
            // Spelling variations
            "school of computer science & applications",
            "school of computer science & application",
            "school of computer science engg. and application",
            "school of computer science engg & app.",
            "school of computer science engg & app",
            "school of commerce and management",
            "school of commerce & management",
            "school of bio-engg and bio-science",
            "school of bio-engg & bio-science",
            "school of bio-engineering & bio science",
            "school of bio-engineering and bio science",
            "school of media and communication studies",
            "school of media & communication studies",
            "school of design",
            "school of applied arts",
            "school of continual education",
            "school of engg. management and research",
            "school of engg. management & research",
            "school of engineering, management & research",
            "school of engineering, management and research"
    );

    public static boolean isValidSchool(String school) {
        if (school == null) return false;
        return VALID_SCHOOLS_LOWERCASE.contains(school.trim().toLowerCase(Locale.ROOT));
    }

    public static String canonicalizeSchool(String school) {
        if (school == null) {
            return null;
        }
        String s = school.trim().toLowerCase(Locale.ROOT);
        if (s.contains("socsea") || s.contains("computer science")) {
            return "SOCSEA";
        }
        if (s.contains("socm") || s.contains("commerce")) {
            return "SOCM";
        }
        if (s.contains("sobb") || s.contains("bio")) {
            return "SOBB";
        }
        if (s.contains("somcs") || s.contains("somcs") || s.contains("media")) {
            return "SOMCS";
        }
        if (s.contains("sod") || s.contains("design")) {
            return "SOD";
        }
        if (s.contains("soaa") || s.contains("applied arts")) {
            return "SOAA";
        }
        if (s.contains("soce") || s.contains("continual") || s.contains("continuing")) {
            return "SOCE";
        }
        if (s.contains("soemr") || s.contains("engineering, management") || s.contains("engineering management") || (s.contains("engg") && s.contains("management") && s.contains("research"))) {
            return "SOEMR";
        }
        return school.trim().toUpperCase(Locale.ROOT); // fallback
    }
}
