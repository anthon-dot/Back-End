package com.code.back_end.repository;

import com.code.back_end.entity.BusinessApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessApplicationRepository
        extends JpaRepository<BusinessApplication, Long> {

    Optional<BusinessApplication> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);
}
