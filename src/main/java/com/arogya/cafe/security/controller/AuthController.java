package com.arogya.cafe.security.controller;

import com.arogya.cafe.common.enums.StaffRole;
import com.arogya.cafe.common.exception.NotFoundException;
import com.arogya.cafe.security.entity.Staff;
import com.arogya.cafe.security.provider.CurrentStaffProvider;
import com.arogya.cafe.security.repository.StaffRepository;
import com.arogya.cafe.security.service.*;
import com.arogya.cafe.security.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record LoginResponse(String token, String username, StaffRole role, Instant expiresAt) {}

    public record MeResponse(Long id, String name, String username, StaffRole role) {}

    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final StaffRepository staff;
    private final CurrentStaffProvider currentStaff;

    public AuthController(
            AuthenticationManager authManager,
            JwtService jwt,
            StaffRepository staff,
            CurrentStaffProvider currentStaff) {
        this.authManager = authManager;
        this.jwt = jwt;
        this.staff = staff;
        this.currentStaff = currentStaff;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // Throws BadCredentialsException (-> 401) on failure.
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.username(), req.password()));
        Staff s = staff.findByUsername(req.username());
        if (s == null) {
            throw new NotFoundException("Staff '" + req.username() + "' not found");
        }
        return new LoginResponse(jwt.generate(s), s.getUsername(), s.getRole(), jwt.expiresAt());
    }

    @GetMapping("/me")
    public MeResponse me() {
        Staff s = currentStaff.require();
        return new MeResponse(s.getId(), s.getName(), s.getUsername(), s.getRole());
    }
}
