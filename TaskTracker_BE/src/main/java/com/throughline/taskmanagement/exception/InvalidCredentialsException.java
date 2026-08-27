package com.throughline.taskmanagement.exception;

/** Thrown on login failure — unknown email, wrong password, or an account with no password set yet. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
