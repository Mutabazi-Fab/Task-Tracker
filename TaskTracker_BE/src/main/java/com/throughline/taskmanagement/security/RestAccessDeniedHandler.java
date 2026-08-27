package com.throughline.taskmanagement.security;

import tools.jackson.databind.ObjectMapper;
import com.throughline.taskmanagement.exception.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 403s for an authenticated user who's still not allowed to do something (role checks land in later phases). */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = ErrorResponseFactory.build(
                HttpStatus.FORBIDDEN, "You don't have permission to perform this action.", request.getRequestURI(), null);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
