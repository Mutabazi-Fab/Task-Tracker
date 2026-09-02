package com.throughline.taskmanagement.security;

import com.throughline.taskmanagement.exception.TooManyAttemptsException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal, in-memory brute-force guard for endpoints where an attacker gets to keep
 * guessing a secret — a password at login, or a 6-digit OTP/reset code at email
 * verification. Keyed by email, not IP: this app has no reverse-proxy/IP-forwarding setup
 * to trust a client-supplied IP header from, and email-keying already stops the actual
 * threat (grinding through passwords/codes against ONE target account), even though it
 * doesn't stop someone spreading low-volume guesses thinly across many accounts. Deliberately
 * simple: single-instance, in-memory only — attempt counts reset on restart and wouldn't be
 * shared across multiple app instances behind a load balancer. Fine at this project's scale;
 * a real multi-instance deployment would need a shared store (Redis, a DB table) instead.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Window(int count, Instant startedAt) {}

    private final Map<String, Window> attemptsByKey = new ConcurrentHashMap<>();

    /** Call before attempting the sensitive operation — throws if this key has hit the
     *  limit within the current window. */
    public void checkAllowed(String key) {
        Window w = attemptsByKey.get(normalize(key));
        if (w != null && w.count() >= MAX_ATTEMPTS && !windowExpired(w)) {
            throw new TooManyAttemptsException("Too many attempts. Please wait a few minutes before trying again.");
        }
    }

    /** Call after a failed attempt (wrong password, wrong/expired code). */
    public void recordFailure(String key) {
        String k = normalize(key);
        attemptsByKey.compute(k, (ignoredKey, existing) -> {
            if (existing == null || windowExpired(existing)) {
                return new Window(1, Instant.now());
            }
            return new Window(existing.count() + 1, existing.startedAt());
        });
    }

    /** Call after a successful attempt — clears any accumulated failures for this key, so
     *  a legitimate login right after a few typos isn't held against them later. */
    public void recordSuccess(String key) {
        attemptsByKey.remove(normalize(key));
    }

    private boolean windowExpired(Window w) {
        return Duration.between(w.startedAt(), Instant.now()).compareTo(WINDOW) >= 0;
    }

    private String normalize(String key) {
        return key.toLowerCase();
    }
}
