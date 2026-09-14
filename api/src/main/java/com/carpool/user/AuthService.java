package com.carpool.user;

import com.carpool.common.ApiException;
import com.carpool.security.AuthUser;
import com.carpool.security.JwtService;
import com.carpool.user.dto.AuthResponse;
import com.carpool.user.dto.LoginRequest;
import com.carpool.user.dto.RegisterRequest;
import com.carpool.user.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle throttle;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
                       LoginThrottle throttle, Clock clock) {
        this.throttle = throttle;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = users.save(new User(email, DisplayNames.clean(request.displayName()),
                passwordEncoder.encode(request.password()), clock.instant()));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        if (throttle.isBlocked(email)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed attempts. Try again in a few minutes.");
        }
        User user = users.findByEmailIgnoreCase(email)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElse(null);
        if (user == null) {
            throttle.failed(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        throttle.succeeded(email);
        return tokenFor(user);
    }

    private AuthResponse tokenFor(User user) {
        String token = jwtService.issue(new AuthUser(user.getId(), user.getEmail()));
        return new AuthResponse(token, jwtService.expiresAt(token), UserResponse.from(user));
    }

    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
