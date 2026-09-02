package com.throughline.taskmanagement.exception;

/** Thrown when a caller has made too many failed attempts against a rate-limited endpoint
 *  (login, email/OTP verification) within the current window — see LoginRateLimiter. */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
