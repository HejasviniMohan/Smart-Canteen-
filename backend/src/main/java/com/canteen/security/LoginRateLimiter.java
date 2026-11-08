package com.canteen.security;

import com.canteen.exception.ApiException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Slows password guessing with two limits: per email (one account being attacked) and per
 * IP (one attacker trying many accounts). Counts live in this instance's memory, in a
 * bounded cache, which is enough for a single-instance deployment.
 */
@Component
public class LoginRateLimiter {

    private final int perEmail;
    private final int perIp;
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public LoginRateLimiter(@Value("${app.security.login-limit-per-email}") int perEmail,
                            @Value("${app.security.login-limit-per-ip}") int perIp) {
        this.perEmail = perEmail;
        this.perIp = perIp;
    }

    public void check(String email, String ip) {
        boolean allowed = bucket("ip:" + ip, perIp).tryConsume(1)
                && bucket("email:" + email.trim().toLowerCase(Locale.ROOT), perEmail).tryConsume(1);
        if (!allowed) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait a minute and try again.");
        }
    }

    private Bucket bucket(String key, int perMinute) {
        return buckets.get(key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build());
    }
}
