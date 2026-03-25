package com.smartmeat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.smartmeat.entity.Review;

@Repository
public  interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByApprovedTrueOrderByCreatedAtDesc();
    List<Review> findAllByOrderByCreatedAtDesc();
    long countByApprovedTrue();

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.approved = true")
    Double averageRating();
}