package com.code.back_end.repository;

import com.code.back_end.entity.Stall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StallRepository
        extends JpaRepository<Stall, Long> {
}