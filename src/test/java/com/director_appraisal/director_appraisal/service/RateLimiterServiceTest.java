package com.director_appraisal.director_appraisal.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RateLimiterServiceTest {

    private StringRedisTemplate redisTemplate;
    private RateLimiterService rateLimiterService;
    private RedisOperations<String, String> mockOps;
    private HashOperations<String, Object, Object> mockHashOps;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        rateLimiterService = new RateLimiterService(redisTemplate);

        // Inject config values
        ReflectionTestUtils.setField(rateLimiterService, "loginCapacity", 5.0);
        ReflectionTestUtils.setField(rateLimiterService, "loginRefillTokens", 5.0);
        ReflectionTestUtils.setField(rateLimiterService, "loginRefillDurationStr", "60s");

        ReflectionTestUtils.setField(rateLimiterService, "forgotCapacity", 5.0);
        ReflectionTestUtils.setField(rateLimiterService, "forgotRefillTokens", 5.0);
        ReflectionTestUtils.setField(rateLimiterService, "forgotRefillDurationStr", "60s");

        mockOps = mock(StringRedisTemplate.class);
        mockHashOps = mock(HashOperations.class);

        when(mockOps.opsForHash()).thenReturn(mockHashOps);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCheckLimitSuccess() {
        // Setup Redis mock to return empty map (meaning full bucket)
        when(mockHashOps.entries(any())).thenReturn(new HashMap<>());

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(mockOps);
        });

        RateLimiterService.RateLimitResult result = rateLimiterService.checkLimit("login:ip:1.2.3.4", "login:user:test", "login");

        assertTrue(result.allowed);
        assertEquals(4, result.remaining); // Max 5, consumed 1 -> 4 remaining
        assertEquals(5, result.limit);
        assertEquals(0, result.retryAfter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCheckLimitExceeded() {
        // Setup Redis mock to return 0 tokens remaining
        Map<Object, Object> ipData = new HashMap<>();
        ipData.put("tokens", "0.0");
        ipData.put("lastRefillTimeMs", String.valueOf(System.currentTimeMillis()));

        when(mockHashOps.entries(any())).thenReturn(ipData);

        when(redisTemplate.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(mockOps);
        });

        RateLimiterService.RateLimitResult result = rateLimiterService.checkLimit("login:ip:1.2.3.4", "login:user:test", "login");

        assertFalse(result.allowed);
        assertEquals(0, result.remaining);
        assertEquals(5, result.limit);
        assertTrue(result.retryAfter > 0);
    }

    @Test
    void testClientIpDetectionPriority() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        // Case 1: X-Forwarded-For present
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        assertEquals("1.2.3.4", rateLimiterService.getClientIp(request));

        // Case 2: X-Forwarded-For malformed/unknown
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown, 2.3.4.5");
        assertEquals("2.3.4.5", rateLimiterService.getClientIp(request));

        // Case 3: X-Forwarded-For absent, X-Real-IP present
        reset(request);
        when(request.getHeader("X-Real-IP")).thenReturn("9.9.9.9");
        assertEquals("9.9.9.9", rateLimiterService.getClientIp(request));

        // Case 4: Both absent, fallback to Remote Address
        reset(request);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        assertEquals("127.0.0.1", rateLimiterService.getClientIp(request));
    }
}
