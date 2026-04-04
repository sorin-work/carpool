package com.carpool.user;

import com.carpool.common.ApiException;
import com.carpool.security.AuthUser;
import com.carpool.user.dto.ChangePasswordRequest;
import com.carpool.user.dto.UpdateProfileRequest;
import com.carpool.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public MeController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public UserResponse me(@AuthenticationPrincipal AuthUser auth) {
        return UserResponse.from(load(auth));
    }

    @PutMapping
    @Transactional
    public UserResponse update(@AuthenticationPrincipal AuthUser auth, @Valid @RequestBody UpdateProfileRequest request) {
        User user = load(auth);
        user.setDisplayName(request.displayName().trim());
        return UserResponse.from(user);
    }

    @PutMapping("/password")
    @Transactional
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser auth,
                               @Valid @RequestBody ChangePasswordRequest request) {
        User user = load(auth);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Your current password is not correct");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User load(AuthUser auth) {
        return users.findById(auth.id()).orElseThrow(() -> ApiException.notFound("User"));
    }
}
