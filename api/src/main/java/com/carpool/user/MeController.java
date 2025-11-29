package com.carpool.user;

import com.carpool.common.ApiException;
import com.carpool.security.AuthUser;
import com.carpool.user.dto.UpdateProfileRequest;
import com.carpool.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository users;

    public MeController(UserRepository users) {
        this.users = users;
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

    private User load(AuthUser auth) {
        return users.findById(auth.id()).orElseThrow(() -> ApiException.notFound("User"));
    }
}
