package com.cinemabook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Phase 1 auth flows.
 * Requires docker-compose up -d (PostgreSQL + Redis) before running.
 *
 * Note: server.servlet.context-path=/api/v1 is set in application.properties.
 * MockMvc with @SpringBootTest uses the full servlet context, so all paths
 * must include the /api/v1 prefix.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    private static final String BASE = "/api/v1";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    @DisplayName("Watcher registration returns 201 with a userId")
    void registerWatcher_success() throws Exception {
        var req = new RegisterWatcherRequest(
                "testuser@example.com", "testuser", "password123", "Test User");

        mockMvc.perform(post(BASE + "/auth/register/watcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.message").isNotEmpty());
    }

    @Test
    @DisplayName("Duplicate email returns 409 Conflict")
    void registerWatcher_duplicateEmail_conflict() throws Exception {
        var req = new RegisterWatcherRequest(
                "dup@example.com", "uniqueuser1", "password123", null);

        mockMvc.perform(post(BASE + "/auth/register/watcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        var dup = new RegisterWatcherRequest(
                "dup@example.com", "uniqueuser2", "password123", null);

        mockMvc.perform(post(BASE + "/auth/register/watcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Email is already registered"));
    }

    @Test
    @DisplayName("Invalid email format returns 400")
    void registerWatcher_invalidEmail_badRequest() throws Exception {
        var req = new RegisterWatcherRequest(
                "not-an-email", "validuser", "password123", null);

        mockMvc.perform(post(BASE + "/auth/register/watcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Film owner registration returns 201")
    void registerFilmOwner_success() throws Exception {
        var req = new RegisterFilmOwnerRequest(
                "owner@studio.com", "mystudio", "password123",
                "My Studio Ltd", "Studio Owner");

        mockMvc.perform(post(BASE + "/auth/register/film-owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Login with wrong password returns 401")
    void login_wrongPassword_unauthorized() throws Exception {
        var reg = new RegisterWatcherRequest(
                "logintest@example.com", "logintest", "correctpass", null);
        mockMvc.perform(post(BASE + "/auth/register/watcher")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)));

        var login = new LoginRequest(
                "logintest@example.com", "wrongpass", "device-abc", "Test Browser");

        mockMvc.perform(post(BASE + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Login with non-existent user returns 401")
    void login_unknownUser_unauthorized() throws Exception {
        var login = new LoginRequest(
                "nobody@example.com", "pass123", "device-xyz", "Test Browser");

        mockMvc.perform(post(BASE + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
