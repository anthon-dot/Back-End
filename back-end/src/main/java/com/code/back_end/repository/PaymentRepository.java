package com.code.back_end.repository;

import com.code.back_end.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByBillingId(
            Long billingId
    );

    List<Payment> findByStakeholderId(
            Long stakeholderId
    );

    List<Payment> findByStakeholder_User_Id(
            Long userId
    );

    Optional<Payment> findByIdAndStakeholder_User_Id(
            Long id,
            Long userId
    );
}
