package com.carpool.common;

import java.time.Instant;
import java.util.Map;

/**
 * The body of every error response. {@code fields} is only present for validation errors and maps a
 * request field to the first message for it.
 */
public record ApiError(Instant timestamp, int status, String error, String message, String path,
                       Map<String, String> fields) {
}
