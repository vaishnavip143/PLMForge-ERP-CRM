package com.plmforge.crm.controller;

import com.plmforge.crm.dto.request.LoginRequest;
import com.plmforge.crm.dto.request.RegisterRequest;
import com.plmforge.crm.dto.response.ApiResponse;
import com.plmforge.crm.dto.response.AuthResponse;
import com.plmforge.crm.dto.response.CurrentUserResponse;
import com.plmforge.crm.dto.response.RegisterResponse;
import com.plmforge.crm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Public registration, login and current user")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer (role is always CUSTOMER)")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Customer registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> me(Authentication authentication) {
        CurrentUserResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Current user retrieved"));
    }
}
