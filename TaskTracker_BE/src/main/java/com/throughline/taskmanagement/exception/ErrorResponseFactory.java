package com.throughline.taskmanagement.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the same error envelope everywhere an error can originate — both
 * GlobalExceptionHandler (controller-thrown exceptions) and the Security
 * entry points (401/403 raised by the JWT filter chain, before a request
 * ever reaches a controller) use this, so a caller sees one consistent
 * shape regardless of which layer rejected the request.
 */
public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static Map<String, Object> build(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("path", path);

        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            response.put("fieldErrors", fieldErrors);
        }

        return response;
    }
}
