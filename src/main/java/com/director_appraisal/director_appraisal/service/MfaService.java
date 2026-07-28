package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.MfaLoginSession;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.MfaLoginSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MfaService {

    private final MfaLoginSessionRepository mfaLoginSessionRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String createMfaSession(User user) {
        String otp = generateNumericOtp();
        String otpHash = passwordEncoder.encode(otp);
        LocalDateTime now = LocalDateTime.now();
        String sessionId = UUID.randomUUID().toString();

        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(user.getId())
                .otpHash(otpHash)
                .createdAt(now)
                .expiresAt(now.plusMinutes(5))
                .used(false)
                .failedAttempts(0)
                .lockedUntil(null)
                .resendCount(0)
                .lastResendAt(null)
                .build();

        mfaLoginSessionRepository.save(session);
        log.info("OTP generated for user ID: {}", user.getId());

        sendOtpEmail(user.getEmail(), otp);
        return sessionId;
    }

    @Transactional
    public Long verifyOtp(String loginSessionId, String otp) {
        if (loginSessionId == null || loginSessionId.isBlank()) {
            throw new IllegalArgumentException("Invalid login session.");
        }
        if (otp == null || !otp.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        MfaLoginSession session = mfaLoginSessionRepository.findById(loginSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login session."));

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(session.getUsed())) {
            throw new IllegalArgumentException("Verification code already used.");
        }

        if (session.getLockedUntil() != null && session.getLockedUntil().isAfter(now)) {
            throw new IllegalStateException("Verification locked due to too many failed attempts. Please try again later.");
        }

        if (session.getExpiresAt().isBefore(now)) {
            log.warn("OTP expired for session: {}", loginSessionId);
            throw new IllegalArgumentException("Verification code expired.");
        }

        if (!passwordEncoder.matches(otp, session.getOtpHash())) {
            int newFailedAttempts = session.getFailedAttempts() + 1;
            session.setFailedAttempts(newFailedAttempts);
            if (newFailedAttempts >= 5) {
                session.setLockedUntil(now.plusMinutes(15));
                log.warn("Session {} locked until 15 mins due to {} failed attempts", loginSessionId, newFailedAttempts);
            }
            mfaLoginSessionRepository.save(session);
            log.warn("OTP verification failed for session: {}", loginSessionId);
            throw new IllegalArgumentException("Invalid verification code.");
        }

        session.setUsed(true);
        mfaLoginSessionRepository.save(session);
        log.info("OTP verified successfully for session: {}", loginSessionId);

        return session.getUserId();
    }

    @Transactional
    public void resendOtp(String loginSessionId, String userEmail) {
        if (loginSessionId == null || loginSessionId.isBlank()) {
            throw new IllegalArgumentException("Invalid login session.");
        }

        MfaLoginSession session = mfaLoginSessionRepository.findById(loginSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid login session."));

        LocalDateTime now = LocalDateTime.now();

        if (Boolean.TRUE.equals(session.getUsed())) {
            throw new IllegalArgumentException("Verification code already used.");
        }

        if (session.getResendCount() >= 3) {
            throw new IllegalArgumentException("Maximum resend attempts reached.");
        }

        if (session.getLastResendAt() != null && session.getLastResendAt().plusSeconds(30).isAfter(now)) {
            throw new IllegalArgumentException("Please wait 30 seconds before requesting a new code.");
        }

        String newOtp = generateNumericOtp();
        String newOtpHash = passwordEncoder.encode(newOtp);

        session.setOtpHash(newOtpHash);
        session.setExpiresAt(now.plusMinutes(5));
        session.setResendCount(session.getResendCount() + 1);
        session.setLastResendAt(now);
        session.setFailedAttempts(0);
        session.setLockedUntil(null);

        mfaLoginSessionRepository.save(session);
        log.info("OTP resent for session: {}", loginSessionId);

        sendOtpEmail(userEmail, newOtp);
    }

    private String generateNumericOtp() {
        int code = secureRandom.nextInt(1000000);
        return String.format("%06d", code);
    }

    private void sendOtpEmail(String email, String otp) {
        String subject = "Login Verification Code";
        String body = String.format(
                "Your verification code is:\n\n%s\n\nThis code expires in 5 minutes.\n\nIf you did not request this login, ignore this email.",
                otp
        );
        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", email, e.getMessage());
        }
    }
}
