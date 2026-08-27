package com.throughline.taskmanagement.security;

import tools.jackson.databind.ObjectMapper;
import com.throughline.taskmanagement.exception.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 401s raised by the security filter chain itself never reach GlobalExceptionHandler — this
 * keeps their body the same shape anyway.
 *
 * Note the import: this Spring Boot version ships Jackson 3.x, which renamed its package from
 * com.fasterxml.jackson.* to tools.jackson.* — this is NOT the classic Jackson 2 ObjectMapper.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = ErrorResponseFactory.build(
                HttpStatus.UNAUTHORIZED, "Authentication is required to access this resource.", request.getRequestURI(), null);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
