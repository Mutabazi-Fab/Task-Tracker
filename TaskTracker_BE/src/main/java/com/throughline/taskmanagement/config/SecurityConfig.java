package com.throughline.taskmanagement.config;

import com.throughline.taskmanagement.security.JwtAuthenticationFilter;
import com.throughline.taskmanagement.security.RestAccessDeniedHandler;
import com.throughline.taskmanagement.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * JWT, stateless. Every route requires a valid Bearer token except signup/login/logout
 * (you need to be able to log in before you have one). Now that the frontend (Phase 7)
 * attaches a token to every request, this is authenticated() everywhere else — it used to
 * be permitAll() while the frontend still couldn't send one.
 *
 * No DaoAuthenticationProvider bean here on purpose: Spring Boot's security
 * auto-configuration builds one automatically from the CustomUserDetailsService
 * and PasswordEncoder beans it finds — constructing it by hand isn't needed
 * and just couples this class to that provider's constructor shape, which
 * has changed across Spring Security versions.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    /**
     * Plain-text, on request — NOT the default and NOT recommended. NoOpPasswordEncoder
     * stores/compares passwords as-is, no hashing at all. Anyone with database access
     * (pgAdmin, a backup, a leak) can read every password directly. Swap back to
     * `new BCryptPasswordEncoder()` to restore hashing — nothing else in the auth code
     * needs to change either way, since it only ever goes through this PasswordEncoder
     * abstraction.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/logout",
                                "/api/v1/auth/verify-email", "/api/v1/auth/resend-otp"
                        ).permitAll()
                        // Phase 7: the frontend now sends a token on every request, so every
                        // other route requires one too. Role/ownership rules beyond "is this
                        // a real logged-in person" still live in each service method (the
                        // requireDirector/requireDirectorOrTeamLeader-style checks) — this
                        // layer only answers "who are you", not "are you allowed to do this".
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
