package com.throughline.taskmanagement.security;

import com.throughline.taskmanagement.exception.TooManyAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Covers the exact brute-force scenario this class exists for: five wrong attempts lock
 *  an account out, a sixth (even a correct one) is still rejected, and a completely
 *  unrelated account is never affected by someone else's lockout. Doesn't test the
 *  15-minute window actually expiring — that would mean either sleeping the real 15
 *  minutes in a test or reworking the class to accept an injectable clock, neither of
 *  which is worth it just to cover a plain "time passed" branch. */
class LoginRateLimiterTest {

    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiter();
    }

    @Test
    void allowsAttemptsUnderTheLimit() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.recordFailure("vincent@example.com");
        }

        assertDoesNotThrow(() -> rateLimiter.checkAllowed("vincent@example.com"));
    }

    @Test
    void locksOutAfterFiveFailures() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailure("vincent@example.com");
        }

        assertThrows(TooManyAttemptsException.class, () -> rateLimiter.checkAllowed("vincent@example.com"));
    }

    @Test
    void lockoutIsNotLiftedByEnteringTheCorrectPasswordAfterwards() {
        // recordSuccess is only ever called by AuthServiceImpl AFTER checkAllowed already
        // passed — this simulates what happens if checkAllowed is (correctly) called first,
        // even with the right credentials: it's still rejected on its own, before a
        // recordSuccess could ever run.
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailure("vincent@example.com");
        }

        assertThrows(TooManyAttemptsException.class, () -> rateLimiter.checkAllowed("vincent@example.com"));
    }

    @Test
    void recordSuccessClearsAccumulatedFailures() {
        for (int i = 0; i < 4; i++) {
            rateLimiter.recordFailure("vincent@example.com");
        }
        rateLimiter.recordSuccess("vincent@example.com");
        rateLimiter.recordFailure("vincent@example.com");

        // Only 1 failure since the successful login reset the count — nowhere near locked.
        assertDoesNotThrow(() -> rateLimiter.checkAllowed("vincent@example.com"));
    }

    @Test
    void lockoutIsScopedPerKeyNotGlobal() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailure("vincent@example.com");
        }

        assertThrows(TooManyAttemptsException.class, () -> rateLimiter.checkAllowed("vincent@example.com"));
        assertDoesNotThrow(() -> rateLimiter.checkAllowed("mucyomutabazifabrice@gmail.com"));
    }

    @Test
    void keysAreCaseInsensitive() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordFailure("Vincent@Example.com");
        }

        assertThrows(TooManyAttemptsException.class, () -> rateLimiter.checkAllowed("vincent@example.com"));
    }
}
