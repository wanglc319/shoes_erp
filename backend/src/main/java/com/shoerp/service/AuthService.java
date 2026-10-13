package com.shoerp.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.dto.LoginRequest;
import com.shoerp.dto.LoginResponse;
import com.shoerp.entity.User;
import com.shoerp.repository.UserRepository;
import com.shoerp.security.JwtTokenProvider;

@Service
public class AuthService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider tokenProvider;

    private final PermissionService permissionService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider, PermissionService permissionService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new BusinessException(400, "Username and password are required");
        }
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException(401, "Invalid username or password"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BusinessException(403, "Account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid username or password");
        }
        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginResponse(token, user.getId(), user.getUsername(), user.getRealName(), user.getRole(),
                permissionService.menusOf(user.getRole()));
    }

    @Transactional(readOnly = true)
    public LoginResponse profile(Long userId) {
        User user = getById(userId);
        return new LoginResponse(null, user.getId(), user.getUsername(), user.getRealName(), user.getRole(),
                permissionService.menusOf(user.getRole()));
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "User not found"));
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (oldPassword == null || newPassword == null) {
            throw new BusinessException(400, "Old password and new password are required");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException(400, "New password must be at least 6 characters");
        }
        if ("123456".equals(newPassword)) {
            throw new BusinessException(400, "New password must not be the default password");
        }
        User user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(400, "Old password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(400, "New password must be different from the old one");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
