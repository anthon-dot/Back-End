package com.code.back_end.reports.repository;

import com.code.back_end.reports.Report;
import com.code.back_end.reports.dto.*;

import com.code.back_end.entity.Payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReportRepository
        extends JpaRepository<Report, Long> {

    List<Report> findBySupervisor_IdOrderByCreatedDateDesc(
            Long supervisorId
    );

    Optional<Report> findByIdAndSupervisor_Id(
            Long id,
            Long supervisorId
    );

    // ====================================
    // MONTHLY REVENUE
    // ====================================

    @Query("""
        SELECT new com.code.back_end.reports.dto.MonthlyRevenueDTO(
            YEAR(p.paymentDate),
            MONTH(p.paymentDate),
            SUM(p.amount)
        )
        FROM Payment p
        WHERE p.paymentType = 'RENT_PAYMENT'
        GROUP BY YEAR(p.paymentDate),
                 MONTH(p.paymentDate)
        ORDER BY YEAR(p.paymentDate) DESC,
                 MONTH(p.paymentDate) DESC
    """)
    List<MonthlyRevenueDTO>
    getMonthlyRevenue();

    // ====================================
    // DAILY COLLECTIONS
    // ====================================

    @Query("""
    SELECT new com.code.back_end.reports.dto.DailyCollectionDTO(
        FUNCTION('DATE', p.paymentDate),
        COUNT(p.id),
        SUM(p.amount)
    )
    FROM Payment p
    GROUP BY FUNCTION('DATE', p.paymentDate)
    ORDER BY FUNCTION('DATE', p.paymentDate) DESC
""")
List<DailyCollectionDTO> getDailyCollections();
    // ====================================
    // OUTSTANDING BALANCES
    // ====================================

    @Query("""
        SELECT new com.code.back_end.reports.dto.OutstandingBalanceDTO(
            b.billingNo,
            s.businessName,
            b.balance,
            b.dueDate
        )
        FROM Billing b
        JOIN b.occupant o
        JOIN o.stakeholder s
        WHERE b.balance > 0
        ORDER BY b.dueDate ASC
    """)
    List<OutstandingBalanceDTO>
    getOutstandingBalances();

    // ====================================
    // OCCUPIED VS VACANT
    // ====================================

    @Query("""
        SELECT new com.code.back_end.reports.dto.OccupancyDTO(
            s.status,
            COUNT(s.id)
        )
        FROM Stall s
        GROUP BY s.status
    """)
    List<OccupancyDTO>
    getOccupancyAnalytics();
    @Query("""
SELECT p
FROM Payment p
WHERE
(:month IS NULL OR MONTH(p.paymentDate) = :month)
AND
(:year IS NULL OR YEAR(p.paymentDate) = :year)
""")
List<Payment> findFilteredPayments(
        Integer month,
        Integer year
);

    // ==========================================
    // FINANCE (NEW)
    // ==========================================
    @Query("SELECT new com.code.back_end.reports.dto.AdvancePaymentDTO(s.businessName, CONCAT(s.firstName, ' ', s.lastName), s.advancePaymentAmount, s.advanceBalance) FROM Stakeholder s WHERE s.advancePaymentAmount > 0")
    List<AdvancePaymentDTO> getAdvancePayments();

    @Query("SELECT new com.code.back_end.reports.dto.PaymentTrendDTO(YEAR(p.paymentDate), MONTH(p.paymentDate), SUM(p.amount)) FROM Payment p GROUP BY YEAR(p.paymentDate), MONTH(p.paymentDate) ORDER BY YEAR(p.paymentDate), MONTH(p.paymentDate)")
    List<PaymentTrendDTO> getPaymentTrends();

    // ==========================================
    // TENANT MANAGEMENT (NEW)
    // ==========================================
    @Query("SELECT new com.code.back_end.reports.dto.VerifiedTenantDTO(s.businessName, CONCAT(s.firstName, ' ', s.lastName), s.approvedOn) FROM Stakeholder s WHERE s.verifiedTenant = true AND s.isArchived = false")
    List<VerifiedTenantDTO> getVerifiedTenants();

    @Query("SELECT new com.code.back_end.reports.dto.PendingApplicantDTO(s.businessName, CONCAT(s.firstName, ' ', s.lastName), s.appliedOn) FROM Stakeholder s WHERE s.applicationStatus = 'PENDING' AND s.isArchived = false")
    List<PendingApplicantDTO> getPendingApplicants();

    @Query("SELECT new com.code.back_end.reports.dto.ExpiringContractDTO(c.contractNo, c.occupant.stakeholder.businessName, c.endDate) FROM Contract c WHERE c.status = 'ACTIVE' ORDER BY c.endDate ASC")
    List<ExpiringContractDTO> getExpiringContracts();

    @Query("SELECT new com.code.back_end.reports.dto.ArchivedTenantDTO(s.businessName, CONCAT(s.firstName, ' ', s.lastName), s.archivedOn) FROM Stakeholder s WHERE s.isArchived = true")
    List<ArchivedTenantDTO> getArchivedTenants();

    // ==========================================
    // STALL ANALYTICS (NEW)
    // ==========================================
    @Query("SELECT new com.code.back_end.reports.dto.StallRevenueDTO(s.stallNo, SUM(p.amount)) FROM Payment p JOIN p.stakeholder st JOIN st.occupant o JOIN o.stall s GROUP BY s.stallNo")
    List<StallRevenueDTO> getRevenuePerStall();

    @Query("SELECT new com.code.back_end.reports.dto.ExpensiveStallDTO(s.stallNo, s.stallType, s.monthlyRent) FROM Stall s ORDER BY s.monthlyRent DESC")
    List<ExpensiveStallDTO> getMostExpensiveStalls();

    @Query("SELECT new com.code.back_end.reports.dto.StallTypeDistributionDTO(s.stallType, COUNT(s)) FROM Stall s GROUP BY s.stallType")
    List<StallTypeDistributionDTO> getStallTypeDistribution();

    // ==========================================
    // OPERATIONAL (NEW)
    // ==========================================
    @Query("SELECT new com.code.back_end.reports.dto.NotificationSentDTO(n.title, n.createdAt, n.stakeholder.businessName) FROM Notification n ORDER BY n.createdAt DESC")
    List<NotificationSentDTO> getNotificationsSent();

    @Query("SELECT new com.code.back_end.reports.dto.NewApplicationDTO(s.businessName, s.appliedOn, s.applicationStatus) FROM Stakeholder s ORDER BY s.appliedOn DESC")
    List<NewApplicationDTO> getNewApplications();

    @Query("SELECT new com.code.back_end.reports.dto.ContractRenewalDTO(c.contractNo, c.occupant.stakeholder.businessName, c.startDate) FROM Contract c ORDER BY c.startDate DESC")
    List<ContractRenewalDTO> getContractRenewals();
}
