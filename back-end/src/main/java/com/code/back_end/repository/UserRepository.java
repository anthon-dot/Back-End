package com.code.back_end.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.code.back_end.entity.User;

import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByUsername(
            String username
    );

    Optional<User> findByResetToken(
            String token
    );
}