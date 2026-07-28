package com.director_appraisal.director_appraisal.service;

import com.director_appraisal.director_appraisal.model.MfaLoginSession;
import com.director_appraisal.director_appraisal.model.User;
import com.director_appraisal.director_appraisal.repository.MfaLoginSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private MfaLoginSessionRepository mfaLoginSessionRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MfaService mfaService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail("test@dypiu.ac.in");
        testUser.setName("Test User");
    }

    @Test
    void testCreateMfaSession_Success() {
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");

        String sessionId = mfaService.createMfaSession(testUser);

        assertNotNull(sessionId);
        assertFalse(sessionId.isBlank());

        ArgumentCaptor<MfaLoginSession> sessionCaptor = ArgumentCaptor.forClass(MfaLoginSession.class);
        verify(mfaLoginSessionRepository).save(sessionCaptor.capture());

        MfaLoginSession saved = sessionCaptor.getValue();
        assertEquals(100L, saved.getUserId());
        assertEquals("hashed_otp", saved.getOtpHash());
        assertFalse(saved.getUsed());
        assertEquals(0, saved.getFailedAttempts());
        assertEquals(0, saved.getResendCount());

        verify(emailService).sendEmail(eq("test@dypiu.ac.in"), eq("Login Verification Code"), contains("Your verification code is:"));
    }

    @Test
    void testVerifyOtp_Success() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .failedAttempts(0)
                .resendCount(0)
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(passwordEncoder.matches("123456", "hashed_otp")).thenReturn(true);

        Long verifiedUserId = mfaService.verifyOtp(sessionId, "123456");

        assertEquals(100L, verifiedUserId);
        assertTrue(session.getUsed());
        verify(mfaLoginSessionRepository).save(session);
    }

    @Test
    void testVerifyOtp_InvalidOtp_IncrementsFailedAttempts() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .failedAttempts(0)
                .resendCount(0)
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(passwordEncoder.matches("999999", "hashed_otp")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mfaService.verifyOtp(sessionId, "999999"));

        assertEquals("Invalid verification code.", ex.getMessage());
        assertEquals(1, session.getFailedAttempts());
        assertFalse(session.getUsed());
        verify(mfaLoginSessionRepository).save(session);
    }

    @Test
    void testVerifyOtp_LocksAfter5FailedAttempts() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .failedAttempts(4)
                .resendCount(0)
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(passwordEncoder.matches("999999", "hashed_otp")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> mfaService.verifyOtp(sessionId, "999999"));

        assertEquals(5, session.getFailedAttempts());
        assertNotNull(session.getLockedUntil());
        assertTrue(session.getLockedUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    void testVerifyOtp_ExpiredOtp() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .used(false)
                .failedAttempts(0)
                .resendCount(0)
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mfaService.verifyOtp(sessionId, "123456"));

        assertEquals("Verification code expired.", ex.getMessage());
    }

    @Test
    void testVerifyOtp_AlreadyUsed() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(true)
                .failedAttempts(0)
                .resendCount(0)
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mfaService.verifyOtp(sessionId, "123456"));

        assertEquals("Verification code already used.", ex.getMessage());
    }

    @Test
    void testResendOtp_CooldownEnforced() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .failedAttempts(0)
                .resendCount(1)
                .lastResendAt(LocalDateTime.now().minusSeconds(10)) // 10 seconds ago (cooldown 30s)
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mfaService.resendOtp(sessionId, "test@dypiu.ac.in"));

        assertEquals("Please wait 30 seconds before requesting a new code.", ex.getMessage());
    }

    @Test
    void testResendOtp_MaxResendLimitReached() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .failedAttempts(0)
                .resendCount(3) // 3 resends already done
                .lastResendAt(LocalDateTime.now().minusSeconds(40))
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mfaService.resendOtp(sessionId, "test@dypiu.ac.in"));

        assertEquals("Maximum resend attempts reached.", ex.getMessage());
    }

    @Test
    void testResendOtp_Success() {
        String sessionId = "session-123";
        MfaLoginSession session = MfaLoginSession.builder()
                .id(sessionId)
                .userId(100L)
                .otpHash("old_hashed_otp")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .failedAttempts(2)
                .resendCount(1)
                .lastResendAt(LocalDateTime.now().minusSeconds(35))
                .build();

        when(mfaLoginSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(passwordEncoder.encode(anyString())).thenReturn("new_hashed_otp");

        mfaService.resendOtp(sessionId, "test@dypiu.ac.in");

        assertEquals(2, session.getResendCount());
        assertEquals("new_hashed_otp", session.getOtpHash());
        assertEquals(0, session.getFailedAttempts());
        assertNotNull(session.getLastResendAt());

        verify(mfaLoginSessionRepository).save(session);
        verify(emailService).sendEmail(eq("test@dypiu.ac.in"), eq("Login Verification Code"), contains("Your verification code is:"));
    }
}
