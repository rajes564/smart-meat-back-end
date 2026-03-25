package com.smartmeat.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartmeat.dto.request.ReviewRequest;
import com.smartmeat.dto.response.ReviewResponse;
import com.smartmeat.entity.Review;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.ExpenseRepository;
import com.smartmeat.repository.KhataAccountRepository;
import com.smartmeat.repository.OrderRepository;
import com.smartmeat.repository.ProductRepository;
import com.smartmeat.repository.ReviewRepository;
import com.smartmeat.repository.ShopSettingsRepository;
import com.smartmeat.repository.StockPurchaseRepository;
import com.smartmeat.repository.UserRepository;
import com.smartmeat.security.SecurityUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//═══════════════════════════════════════════════════════════════════════════════
//REVIEW SERVICE
//═══════════════════════════════════════════════════════════════════════════════
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

 private final ReviewRepository reviewRepo;
 private final SseService sseService;
 private final SecurityUtils securityUtils;

 public List<ReviewResponse> getApproved() {
     return reviewRepo.findByApprovedTrueOrderByCreatedAtDesc().stream()
             .map(this::toResponse).collect(Collectors.toList());
 }

 public List<ReviewResponse> getAll() {
     return reviewRepo.findAllByOrderByCreatedAtDesc().stream()
             .map(this::toResponse).collect(Collectors.toList());
 }

 @Transactional
 public ReviewResponse submit(ReviewRequest req) {
     Review review = Review.builder()
             .rating(req.getRating())
             .comment(req.getComment())
             .submitterName(req.getName())
             .tags(req.getTags() != null ? req.getTags().toArray(new String[0]) : new String[0])
             .approved(true)
             .build();

     // Link to logged-in user if available
     try {
         review.setUser(securityUtils.currentUser());
     } catch (Exception ignored) {}

     Review saved = reviewRepo.save(review);
     sseService.newReview(toResponse(saved));
     return toResponse(saved);
 }

 @Transactional
 public ReviewResponse approve(Long id) {
     Review r = reviewRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review not found"));
     r.setApproved(true);
     return toResponse(reviewRepo.save(r));
 }

 @Transactional
 public void delete(Long id) {
     reviewRepo.deleteById(id);
 }

 private ReviewResponse toResponse(Review r) {
     return ReviewResponse.builder()
             .id(r.getId())
             .name(r.getSubmitterName() != null ? r.getSubmitterName()
                     : (r.getUser() != null ? r.getUser().getName() : "Anonymous"))
             .rating(r.getRating())
             .comment(r.getComment())
             .tags(r.getTags() != null ? List.of(r.getTags()) : List.of())
             .approved(r.isApproved())
             .createdAt(r.getCreatedAt())
             .build();
 }
}