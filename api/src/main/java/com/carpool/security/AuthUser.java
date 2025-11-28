package com.carpool.security;

/** The authenticated principal, resolved from the bearer token on every request. */
public record AuthUser(Long id, String email) {
}
