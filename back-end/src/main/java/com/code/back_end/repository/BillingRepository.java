// =======================================
// BillingRepository.java
// =======================================
package com.code.back_end.repository;

import com.code.back_end.entity.Billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BillingRepository
        extends JpaRepository<Billing, Long> {

    // =========================
    // CHECK DUPLICATE BILLING
    // =========================
    boolean existsByContractIdAndBillingPeriod(
            Long contractId,
            String billingPeriod
    );

    // =========================
    // FIND BILLINGS BY OCCUPANT
    // =========================
    List<Billing> findByOccupantId(
            Long occupantId
    );

    // =========================
    // FIND BILLINGS BY CONTRACT
    // =========================
    List<Billing> findByContractId(
            Long contractId
    );

    // =========================
    // FIND BILLINGS BY STATUS
    // =========================
    List<Billing> findByStatus(
            String status
    );

    // =========================
    // FIND BILLINGS BY STAKEHOLDER
    // =========================
    List<Billing>
    findByOccupant_Stakeholder_Id(
            Long stakeholderId
    );

    // =========================
    // SORTED BY DUE DATE
    // =========================
    List<Billing>
    findByOccupant_Stakeholder_IdOrderByDueDateAsc(
            Long stakeholderId
    );

    // =========================
    // FIND BY BILLING NUMBER
    // =========================
    Optional<Billing> findByBillingNo(
            String billingNo
    );

    List<Billing>
    findByOccupant_Stakeholder_User_IdOrderByDueDateAsc(
            Long userId
    );
    List<Billing> findByContractIdOrderByDueDateAsc(
        Long contractId
);

    Optional<Billing>
    findByIdAndOccupant_Stakeholder_User_Id(
            Long id,
            Long userId
    );

    Optional<Billing>
    findByBillingNoAndOccupant_Stakeholder_User_Id(
            String billingNo,
            Long userId
    );
}
