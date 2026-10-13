package com.shoerp.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.dto.ChangePasswordRequest;
import com.shoerp.dto.LoginRequest;
import com.shoerp.dto.LoginResponse;
import com.shoerp.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me(Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(authService.profile(userId));
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(Authentication authentication,
                                            @RequestBody ChangePasswordRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.success(null);
    }
}
