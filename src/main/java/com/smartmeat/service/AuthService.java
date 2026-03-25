package com.smartmeat.service;

import com.smartmeat.dto.request.LoginRequest;
import com.smartmeat.dto.request.RegisterRequest;
import com.smartmeat.dto.response.AuthResponse;
import com.smartmeat.entity.User;
import com.smartmeat.enums.Role;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.repository.UserRepository;
import com.smartmeat.security.JwtUtil;
import com.smartmeat.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecurityUtils securityUtils;

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByMobile(req.getMobile())
                .orElseThrow(() -> new BusinessException("Invalid mobile number or password"));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated. Contact admin.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid mobile number or password");
        }

        String token = jwtUtil.generate(user.getMobile(), user.getRole().name(), user.getId());
        log.info("User logged in: {} ({})", user.getMobile(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .userId(user.getId())
                .name(user.getName())
                .mobile(user.getMobile())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByMobile(req.getMobile())) {
            throw new BusinessException("Mobile number already registered");
        }

        User user = User.builder()
                .name(req.getName())
                .mobile(req.getMobile())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        String token = jwtUtil.generate(saved.getMobile(), saved.getRole().name(), saved.getId());
        log.info("New customer registered: {}", saved.getMobile());

        return AuthResponse.builder()
                .token(token)
                .role(saved.getRole().name())
                .userId(saved.getId())
                .name(saved.getName())
                .mobile(saved.getMobile())
                .build();
    }

    public AuthResponse refresh(String oldToken) {
        if (!jwtUtil.isValid(oldToken)) {
            throw new BusinessException("Invalid or expired refresh token");
        }
        String mobile = jwtUtil.extractUsername(oldToken);
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new BusinessException("User not found"));

        String newToken = jwtUtil.generate(user.getMobile(), user.getRole().name(), user.getId());
        return AuthResponse.builder()
                .token(newToken)
                .role(user.getRole().name())
                .userId(user.getId())
                .name(user.getName())
                .build();
    }

    public AuthResponse currentUser() {
        User user = securityUtils.currentUser();
        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .mobile(user.getMobile())
                .role(user.getRole().name())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
