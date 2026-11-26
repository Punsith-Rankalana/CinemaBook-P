package com.cinemabook.infrastructure.security;

import com.cinemabook.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Runs once per request. Validates the JWT, then checks that
 * the device in the token matches the active device in Redis.
 *
 * If the same account is used on a second device, the Redis value
 * has already been overwritten → the first device's token is rejected here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String token = extractBearerToken(request);

        if (token == null) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (!jwtService.isAccessTokenValid(token)) {
                writeError(response, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                return;
            }

            UUID userId   = jwtService.extractUserId(token);
            String deviceId  = jwtService.extractDeviceId(token);
            String userType  = jwtService.extractUserType(token);

            // ── Single-device check ────────────────────────────────
            if (!sessionService.isDeviceAuthorised(userId, deviceId)) {
                log.warn("Device mismatch or session revoked for userId={}", userId);
                writeError(response, HttpStatus.UNAUTHORIZED,
                        "Session expired — your account was logged in on another device");
                return;
            }

            // ── Set Spring Security context ────────────────────────
            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + userType))
            );
            auth.setDetails(deviceId);
            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (JwtException e) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Token error: " + e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(message));
    }
}
