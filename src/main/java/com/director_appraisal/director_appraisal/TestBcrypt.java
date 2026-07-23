package com.director_appraisal.director_appraisal;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123456";
        String hash = "$2a$10$P8x85FYSpm8xYTLKL/52R.6MhKtCwmiICN2A7tqLDh6rDEsrHtV1W";
        boolean match = encoder.matches(rawPassword, hash);
        System.out.println("Match result: " + match);
        System.out.println("New generated hash: " + encoder.encode(rawPassword));
    }
}
