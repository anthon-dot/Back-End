package com.code.back_end.reports.controller;

import com.code.back_end.reports.dto.*;
import com.code.back_end.reports.service.ReportService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService
    ) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ReportRequest request
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        reportService.createReport(
                                authorizationHeader,
                                request
                        )
                );
    }

    @GetMapping
    public List<ReportResponse> getReports(
            @RequestHeader("Authorization") String authorizationHeader
    ) {

        return reportService.getReports(
                authorizationHeader
        );
    }

    @GetMapping("/{id}")
    public ReportResponse getReportById(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id
    ) {

        return reportService.getReportById(
                authorizationHeader,
                id
        );
    }

    @PutMapping("/{id}")
    public ReportResponse updateReport(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest request
    ) {

        return reportService.updateReport(
                authorizationHeader,
                id,
                request
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id
    ) {

        reportService.deleteReport(
                authorizationHeader,
                id
        );

        return ResponseEntity.noContent().build();
    }

    // ====================================
    // MONTHLY REVENUE
    // ====================================

    @GetMapping("/monthly-revenue")
    public List<MonthlyRevenueDTO>
    getMonthlyRevenue() {

        return reportService
                .getMonthlyRevenue();
    }

    // ====================================
    // DAILY COLLECTIONS
    // ====================================

    @GetMapping("/daily-collections")
    public List<DailyCollectionDTO>
    getDailyCollections() {

        return reportService
                .getDailyCollections();
    }

    // ====================================
    // OUTSTANDING BALANCES
    // ====================================

    @GetMapping("/outstanding-balances")
    public List<OutstandingBalanceDTO>
    getOutstandingBalances() {

        return reportService
                .getOutstandingBalances();
    }

    // ====================================
    // OCCUPANCY ANALYTICS
    // ====================================

    @GetMapping("/occupancy")
    public List<OccupancyDTO>
    getOccupancyAnalytics() {

        return reportService
                .getOccupancyAnalytics();
    }

    @GetMapping("/finance/advance-payments")
    public List<AdvancePaymentDTO> getAdvancePayments() {
        return reportService.getAdvancePayments();
    }

    @GetMapping("/finance/payment-trends")
    public List<PaymentTrendDTO> getPaymentTrends() {
        return reportService.getPaymentTrends();
    }

    @GetMapping("/tenant/verified")
    public List<VerifiedTenantDTO> getVerifiedTenants() {
        return reportService.getVerifiedTenants();
    }

    @GetMapping("/tenant/pending")
    public List<PendingApplicantDTO> getPendingApplicants() {
        return reportService.getPendingApplicants();
    }

    @GetMapping("/tenant/expiring-contracts")
    public List<ExpiringContractDTO> getExpiringContracts() {
        return reportService.getExpiringContracts();
    }

    @GetMapping("/tenant/archived")
    public List<ArchivedTenantDTO> getArchivedTenants() {
        return reportService.getArchivedTenants();
    }

    @GetMapping("/stall/revenue")
    public List<StallRevenueDTO> getRevenuePerStall() {
        return reportService.getRevenuePerStall();
    }

    @GetMapping("/stall/expensive")
    public List<ExpensiveStallDTO> getMostExpensiveStalls() {
        return reportService.getMostExpensiveStalls();
    }

    @GetMapping("/stall/distribution")
    public List<StallTypeDistributionDTO> getStallTypeDistribution() {
        return reportService.getStallTypeDistribution();
    }

    @GetMapping("/operational/notifications")
    public List<NotificationSentDTO> getNotificationsSent() {
        return reportService.getNotificationsSent();
    }

    @GetMapping("/operational/new-applications")
    public List<NewApplicationDTO> getNewApplications() {
        return reportService.getNewApplications();
    }

    @GetMapping("/operational/contract-renewals")
    public List<ContractRenewalDTO> getContractRenewals() {
        return reportService.getContractRenewals();
    }
    
}
