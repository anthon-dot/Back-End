package com.code.back_end.repository;

import com.code.back_end.entity.Stakeholder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StakeholderRepository
        extends JpaRepository<Stakeholder, Long> {

    Optional<Stakeholder> findByUser_Id(
            Long userId
    );

    Optional<Stakeholder> findByIdAndUser_Id(
            Long id,
            Long userId
    );

    List<Stakeholder> findByMarketApprovalStatus(
            String marketApprovalStatus
    );

    List<Stakeholder> findByEndorsementStatus(
            String endorsementStatus
    );

    List<Stakeholder> findByBploStatus(
            String bploStatus
    );

    @Query("""
    SELECT s FROM Stakeholder s
    WHERE (
        s.applicationStatus IN (
            'PENDING_TREASURER_APPROVAL',
            'PENDING_BUSINESS_PERMIT_PAYMENT'
        )
        OR (
            s.treasurerApproved = false
            AND s.applicationStatus NOT IN ('COMPLETED', 'REJECTED')
        )
    )
    AND s.applicationStatus NOT IN ('COMPLETED', 'FULLY_APPROVED', 'REJECTED')
    """)
    List<Stakeholder> findForApproval();

    @Modifying(flushAutomatically = true)
    @Query("""
    UPDATE Stakeholder s
    SET s.applicantFeePaid = true,
        s.applicantFeeAmount = :amount,
        s.applicantFeeDate = :feeDate,
        s.verifiedStakeholder = true,
        s.verifiedTenant = true,
        s.verificationDate = :verificationDate
    WHERE s.id = :id
    """)
    int markApplicantFeePaid(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount,
            @Param("feeDate") LocalDate feeDate,
            @Param("verificationDate") LocalDateTime verificationDate
    );
}
