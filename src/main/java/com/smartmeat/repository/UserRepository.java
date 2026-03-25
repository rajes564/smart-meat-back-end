package com.smartmeat.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import com.smartmeat.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByMobile(String mobile);
    Optional<User> findByEmail(String email);
    boolean existsByMobile(String mobile);
    
    List<User> findAllByOrderByCreatedAtDesc();
}



