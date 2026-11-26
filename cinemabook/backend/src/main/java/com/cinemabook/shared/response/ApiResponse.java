package com.cinemabook.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;

/**
 * Every API response is wrapped in this envelope.
 *
 * Success:  { success: true,  data: {...},  error: null  }
 * Failure:  { success: false, data: null,   error: "..." }
 *
 * Open/Closed: new response types are new static factory methods —
 * the class structure never changes.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String error;
    private final Instant timestamp;

    private ApiResponse(boolean success, T data, String error) {
        this.success   = success;
        this.data      = data;
        this.error     = error;
        this.timestamp = Instant.now();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, null, null);
    }
}
