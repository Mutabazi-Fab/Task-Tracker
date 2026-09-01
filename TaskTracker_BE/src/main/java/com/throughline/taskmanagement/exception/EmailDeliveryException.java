package com.throughline.taskmanagement.exception;

/** Thrown when sending a real email (an OTP, an invite) fails — falls through to
 *  GlobalExceptionHandler's generic 500 handler, which is honest here: this really is an
 *  upstream (mail server) failure, not a problem with what the client sent. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message) {
        super(message);
    }
}
