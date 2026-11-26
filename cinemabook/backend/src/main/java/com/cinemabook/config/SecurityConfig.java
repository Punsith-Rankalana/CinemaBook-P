package com.cinemabook.config;

import com.cinemabook.infrastructure.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final String        allowedOrigins;

    public SecurityConfig(
            JwtAuthFilter jwtAuthFilter,
            @Value("${cinemabook.cors.allowed-origins}") String allowedOrigins) {
        this.jwtAuthFilter  = jwtAuthFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public auth endpoints ──────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register/watcher",
                                "/auth/register/film-owner",
                                "/auth/login",
                                "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/auth/verify-email").permitAll()

                        // ── Health check ───────────────────────────────────
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Dev file upload (PUT, no JWT needed) ───────────
                        // Storage key is a UUID path only the owner received.
                        // In production this route does not exist — S3 handles it.
                        .requestMatchers(HttpMethod.PUT, "/files/upload/**").permitAll()

                        // ── Video stream (GET, no JWT header — token is in URL) ──
                        // The <video> element cannot send Authorization headers.
                        // Authentication is done inside StreamingController via
                        // the signed JWT embedded in the ?token= query parameter.
                        // The token is short-lived (4 hours) and signed with the
                        // same secret as all other JWTs — secure without a header.
                        .requestMatchers(HttpMethod.GET, "/files/stream").permitAll()

                        // ── Clip downloads (GET, no JWT needed) ───────────
                        // Clips are intentionally downloadable — they are watermarked
                        // server-side with the watcher's userId + timestamp.
                        .requestMatchers(HttpMethod.GET, "/files/clips/**").permitAll()

                        // ── Everything else requires a valid JWT ───────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}