package com.director_appraisal.director_appraisal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    @Value("${rate-limit.login.capacity:5}")
    private double loginCapacity;

    @Value("${rate-limit.login.refillTokens:5}")
    private double loginRefillTokens;

    @Value("${rate-limit.login.refillDuration:60s}")
    private String loginRefillDurationStr;

    @Value("${rate-limit.forgot.capacity:5}")
    private double forgotCapacity;

    @Value("${rate-limit.forgot.refillTokens:5}")
    private double forgotRefillTokens;

    @Value("${rate-limit.forgot.refillDuration:60s}")
    private String forgotRefillDurationStr;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public static class RateLimitResult {
        public final boolean allowed;
        public final long remaining;
        public final long limit;
        public final long retryAfter;

        public RateLimitResult(boolean allowed, long remaining, long limit, long retryAfter) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.limit = limit;
            this.retryAfter = retryAfter;
        }
    }

    public RateLimitResult checkLimit(String ipKey, String userKey, String type) {
        double capacity = "login".equalsIgnoreCase(type) ? loginCapacity : forgotCapacity;
        double refillTokens = "login".equalsIgnoreCase(type) ? loginRefillTokens : forgotRefillTokens;
        String durationStr = "login".equalsIgnoreCase(type) ? loginRefillDurationStr : forgotRefillDurationStr;
        long durationSec = parseDurationToSeconds(durationStr);

        int maxRetries = 5;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            final double[] remainingHolder = new double[1];
            List<Object> txResult = redisTemplate.execute(new SessionCallback<List<Object>>() {
                @Override
                @SuppressWarnings("unchecked")
                public <K, V> List<Object> execute(org.springframework.data.redis.core.RedisOperations<K, V> operations) {
                    StringRedisTemplate stringOps = (StringRedisTemplate) operations;
                    stringOps.watch(List.of(ipKey, userKey));

                    long nowMs = System.currentTimeMillis();

                    Map<Object, Object> ipData = stringOps.opsForHash().entries(ipKey);
                    Map<Object, Object> userData = stringOps.opsForHash().entries(userKey);

                    double ipTokens = getTokens(ipData, capacity, refillTokens, durationSec, nowMs);
                    double userTokens = getTokens(userData, capacity, refillTokens, durationSec, nowMs);

                    if (ipTokens >= 1.0 && userTokens >= 1.0) {
                        double nextIpTokens = ipTokens - 1.0;
                        double nextUserTokens = userTokens - 1.0;
                        remainingHolder[0] = Math.min(nextIpTokens, nextUserTokens);

                        stringOps.multi();
                        stringOps.opsForHash().put(ipKey, "tokens", String.valueOf(nextIpTokens));
                        stringOps.opsForHash().put(ipKey, "lastRefillTimeMs", String.valueOf(nowMs));
                        stringOps.expire(ipKey, Duration.ofSeconds(durationSec));

                        stringOps.opsForHash().put(userKey, "tokens", String.valueOf(nextUserTokens));
                        stringOps.opsForHash().put(userKey, "lastRefillTimeMs", String.valueOf(nowMs));
                        stringOps.expire(userKey, Duration.ofSeconds(durationSec));

                        return stringOps.exec();
                    } else {
                        stringOps.unwatch();
                        double minTokens = Math.min(ipTokens, userTokens);
                        long retryAfter = 0;
                        if (minTokens < 1.0) {
                            double needed = 1.0 - minTokens;
                            double refillRatePerMs = refillTokens / (durationSec * 1000.0);
                            double waitMs = needed / refillRatePerMs;
                            retryAfter = (long) Math.ceil(waitMs / 1000.0);
                        }
                        return List.of("REJECTED", String.valueOf(retryAfter), String.valueOf((long) Math.floor(minTokens)));
                    }
                }
            });

            if (txResult == null) {
                // Transaction failed due to concurrent modification, retry
                continue;
            }

            if (txResult.size() >= 3 && "REJECTED".equals(txResult.get(0))) {
                long retryAfter = Long.parseLong((String) txResult.get(1));
                long remaining = Long.parseLong((String) txResult.get(2));
                return new RateLimitResult(false, remaining, (long) capacity, retryAfter);
            }

            // Transaction succeeded!
            long remaining = (long) Math.floor(remainingHolder[0]);
            return new RateLimitResult(true, remaining, (long) capacity, 0);
        }

        // If all retries failed due to high concurrency, we default to block
        return new RateLimitResult(false, 0, (long) capacity, durationSec);
    }

    private double getTokens(Map<Object, Object> data, double capacity, double refillTokens, long durationSec, long nowMs) {
        if (data == null || data.isEmpty()) {
            return capacity;
        }
        try {
            double tokens = Double.parseDouble((String) data.get("tokens"));
            long lastRefillTimeMs = Long.parseLong((String) data.get("lastRefillTimeMs"));
            long elapsedMs = nowMs - lastRefillTimeMs;
            if (elapsedMs < 0) elapsedMs = 0;

            double refillRatePerMs = refillTokens / (durationSec * 1000.0);
            double newTokens = tokens + (elapsedMs * refillRatePerMs);
            return Math.min(capacity, newTokens);
        } catch (Exception e) {
            return capacity;
        }
    }

    private long parseDurationToSeconds(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return 60;
        }
        durationStr = durationStr.trim().toLowerCase();
        if (durationStr.endsWith("s")) {
            return Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
        } else if (durationStr.endsWith("m")) {
            return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 60;
        } else if (durationStr.endsWith("h")) {
            return Long.parseLong(durationStr.substring(0, durationStr.length() - 1)) * 3600;
        }
        try {
            return Long.parseLong(durationStr);
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    public String getClientIp(jakarta.servlet.http.HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            for (String ip : ips) {
                String trimmed = ip.trim();
                if (isValidIp(trimmed)) {
                    return trimmed;
                }
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            String trimmed = xRealIp.trim();
            if (isValidIp(trimmed)) {
                return trimmed;
            }
        }
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        if ("unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return ip.contains(".") || ip.contains(":");
    }
}
