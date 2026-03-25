package com.smartmeat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmeat.dto.request.UserCreateRequest;
import com.smartmeat.dto.request.UserUpdateRequest;
import com.smartmeat.dto.response.UserResponse;
import com.smartmeat.entity.User;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.ExpenseRepository;
import com.smartmeat.repository.KhataAccountRepository;
import com.smartmeat.repository.OrderRepository;
import com.smartmeat.repository.ProductRepository;
import com.smartmeat.repository.StockPurchaseRepository;
import com.smartmeat.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//═══════════════════════════════════════════════════════════════════════════════
//USER SERVICE  (admin-only)
//═══════════════════════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

 private final UserRepository userRepo;
 private final PasswordEncoder passwordEncoder;

 public List<UserResponse> getAll() {
     return userRepo.findAllByOrderByCreatedAtDesc().stream()
             .map(this::toResponse).collect(Collectors.toList());
 }

 @Transactional
 public UserResponse create(UserCreateRequest req) {
     if (userRepo.existsByMobile(req.getMobile())) {
         throw new BusinessException("Mobile already exists");
     }
     User u = User.builder()
             .name(req.getName())
             .mobile(req.getMobile())
             .email(req.getEmail())
             .password(passwordEncoder.encode(req.getPassword()))
             .role(com.smartmeat.enums.Role.valueOf(req.getRole().toUpperCase()))
             .active(true)
             .build();
     return toResponse(userRepo.save(u));
 }

 @Transactional
 public UserResponse update(Long id, UserUpdateRequest req) {
     User u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
     if (req.getName() != null)     u.setName(req.getName());
     if (req.getEmail() != null)    u.setEmail(req.getEmail());
     if (req.getPassword() != null && !req.getPassword().isBlank()) {
         u.setPassword(passwordEncoder.encode(req.getPassword()));
     }
     u.setActive(req.isActive());
     return toResponse(userRepo.save(u));
 }

 @Transactional
 public void delete(Long id) {
     userRepo.deleteById(id);
 }

 @Transactional
 public UserResponse changeRole(Long id, String role) {
     User u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
     u.setRole(com.smartmeat.enums.Role.valueOf(role.toUpperCase()));
     return toResponse(userRepo.save(u));
 }

 private UserResponse toResponse(User u) {
     return UserResponse.builder()
             .id(u.getId()).name(u.getName()).mobile(u.getMobile())
             .email(u.getEmail()).role(u.getRole().name())
             .avatarUrl(u.getAvatarUrl()).active(u.isActive())
             .createdAt(u.getCreatedAt())
             .build();
 }
}
