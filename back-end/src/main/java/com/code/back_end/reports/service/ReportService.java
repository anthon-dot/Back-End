package com.code.back_end.reports.service;

import com.code.back_end.entity.User;
import com.code.back_end.repository.UserRepository;
import com.code.back_end.reports.Report;
import com.code.back_end.reports.dto.*;
import com.code.back_end.reports.repository.ReportRepository;
import com.code.back_end.security.SecurityService;
import com.code.back_end.util.JwtUtil;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final SecurityService securityService;

    public ReportService(
            ReportRepository reportRepository,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            SecurityService securityService
    ) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.securityService = securityService;
    }

    public ReportResponse createReport(
            String authorizationHeader,
            ReportRequest request
    ) {

        User supervisor =
                getMarketSupervisor(authorizationHeader);

        Report report = new Report();
        applyRequest(report, request);
        report.setSupervisor(supervisor);
        report.setSupervisorName(supervisor.getUsername());

        return new ReportResponse(
                reportRepository.save(report)
        );
    }

    public List<ReportResponse> getReports(
            String authorizationHeader
    ) {

        User supervisor =
                getMarketSupervisor(authorizationHeader);

        return reportRepository
                .findBySupervisor_IdOrderByCreatedDateDesc(
                        supervisor.getId()
                )
                .stream()
                .map(ReportResponse::new)
                .toList();
    }

    public ReportResponse getReportById(
            String authorizationHeader,
            Long id
    ) {

        User supervisor =
                getMarketSupervisor(authorizationHeader);

        return new ReportResponse(
                getOwnedReport(id, supervisor)
        );
    }

    public ReportResponse updateReport(
            String authorizationHeader,
            Long id,
            ReportRequest request
    ) {

        User supervisor =
                getMarketSupervisor(authorizationHeader);

        Report report =
                getOwnedReport(id, supervisor);

        applyRequest(report, request);

        return new ReportResponse(
                reportRepository.save(report)
        );
    }

    public void deleteReport(
            String authorizationHeader,
            Long id
    ) {

        User supervisor =
                getMarketSupervisor(authorizationHeader);

        Report report =
                getOwnedReport(id, supervisor);

        reportRepository.delete(report);
    }

    private void applyRequest(
            Report report,
            ReportRequest request
    ) {

        report.setTitle(
                request.getTitle().trim()
        );

        report.setDescription(
                request.getDescription()
        );

        report.setStatus(
                request.getStatus().trim()
        );
    }

    private Report getOwnedReport(
            Long id,
            User supervisor
    ) {

        return reportRepository
                .findByIdAndSupervisor_Id(
                        id,
                        supervisor.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Report not found"
                        )
                );
    }

    private User getMarketSupervisor(
            String authorizationHeader
    ) {

        securityService.requireAdmin();

        return securityService.currentUser();
    }

    // ====================================
    // MONTHLY REVENUE
    // ====================================

    public List<MonthlyRevenueDTO>
    getMonthlyRevenue() {

        return reportRepository
                .getMonthlyRevenue();
    }

    // ====================================
    // DAILY COLLECTIONS
    // ====================================

    public List<DailyCollectionDTO>
    getDailyCollections() {

        return reportRepository
                .getDailyCollections();
    }

    // ====================================
    // OUTSTANDING BALANCES
    // ====================================

    public List<OutstandingBalanceDTO>
    getOutstandingBalances() {

        return reportRepository
                .getOutstandingBalances();
    }

    // ====================================
    // OCCUPANCY ANALYTICS
    // ====================================

    public List<OccupancyDTO>
    getOccupancyAnalytics() {

        return reportRepository
                .getOccupancyAnalytics();
    }

    public List<AdvancePaymentDTO> getAdvancePayments() {
        return reportRepository.getAdvancePayments();
    }

    public List<PaymentTrendDTO> getPaymentTrends() {
        return reportRepository.getPaymentTrends();
    }

    public List<VerifiedTenantDTO> getVerifiedTenants() {
        return reportRepository.getVerifiedTenants();
    }

    public List<PendingApplicantDTO> getPendingApplicants() {
        return reportRepository.getPendingApplicants();
    }

    public List<ExpiringContractDTO> getExpiringContracts() {
        return reportRepository.getExpiringContracts();
    }

    public List<ArchivedTenantDTO> getArchivedTenants() {
        return reportRepository.getArchivedTenants();
    }

    public List<StallRevenueDTO> getRevenuePerStall() {
        return reportRepository.getRevenuePerStall();
    }

    public List<ExpensiveStallDTO> getMostExpensiveStalls() {
        return reportRepository.getMostExpensiveStalls();
    }

    public List<StallTypeDistributionDTO> getStallTypeDistribution() {
        return reportRepository.getStallTypeDistribution();
    }

    public List<NotificationSentDTO> getNotificationsSent() {
        return reportRepository.getNotificationsSent();
    }

    public List<NewApplicationDTO> getNewApplications() {
        return reportRepository.getNewApplications();
    }

    public List<ContractRenewalDTO> getContractRenewals() {
        return reportRepository.getContractRenewals();
    }
}
