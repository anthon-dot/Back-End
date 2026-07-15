package com.code.back_end.service;

import com.code.back_end.dto.*;
import com.code.back_end.entity.Billing;
import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Payment;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.Stall;
import com.code.back_end.repository.BillingRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.PaymentRepository;
import com.code.back_end.repository.StakeholderRepository;
import com.code.back_end.repository.StallRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ReportAIService {

    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final StakeholderRepository stakeholderRepository;
    private final StallRepository stallRepository;
    private final ContractRepository contractRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    public ReportAIService(
            BillingRepository billingRepository,
            PaymentRepository paymentRepository,
            StakeholderRepository stakeholderRepository,
            StallRepository stallRepository,
            ContractRepository contractRepository,
            ObjectMapper objectMapper
    ) {
        this.billingRepository = billingRepository;
        this.paymentRepository = paymentRepository;
        this.stakeholderRepository = stakeholderRepository;
        this.stallRepository = stallRepository;
        this.contractRepository = contractRepository;
        this.objectMapper = objectMapper;
    }

    public AIReportSummaryDTO getSummary() {
        List<Billing> billings = billingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();
        List<Stakeholder> stakeholders = stakeholderRepository.findAll();

        FinancialInsightsDTO financial = getFinancialInsights();
        OccupancyAnalysisDTO occupancy = getOccupancyAnalysis();
        PaymentAnalysisDTO payment = getPaymentAnalysis();

        AIReportSummaryDTO summary = new AIReportSummaryDTO();
        summary.setTotalCollected(financial.getTotalCollected());
        summary.setOutstandingBalance(financial.getOutstandingBalance());
        summary.setOverdueTenants(countOverdueBillings(billings));
        summary.setPendingApplications(
                stakeholders.stream()
                        .filter(stakeholder -> "PENDING".equalsIgnoreCase(
                                safe(stakeholder.getApplicationStatus())
                        ))
                        .count()
        );
        summary.setOccupancyRate(occupancy.getOccupancyRate());
        summary.setMonthlyTrends(buildMonthlyTrends(payments));

        summary.setSummary(
                "Collections are at " + formatMoney(financial.getTotalCollected()) +
                        " with " + formatPercent(financial.getCollectionRate()) +
                        " collection efficiency. " + summary.getOverdueTenants() +
                        " billing account(s) currently need overdue follow-up."
        );

        summary.getInsights().addAll(financial.getInsights());
        summary.getInsights().add(new AIInsightDTO(
                "Occupancy",
                occupancy.getSummary(),
                occupancy.getOccupancyRate() >= 90 ? "success" : "warning"
        ));
        summary.getInsights().add(new AIInsightDTO(
                "Payment trend",
                payment.getSummary(),
                payment.getPercentageChange() >= 0 ? "success" : "warning"
        ));

        summary.getRecommendations().addAll(financial.getRecommendations());
        summary.getRecommendations().addAll(occupancy.getRecommendations());

        return summary;
    }

    public AIReportDTO generateReport(String reportType) {
        String normalized = safe(reportType).isBlank()
                ? "occupancy"
                : reportType.toLowerCase(Locale.ENGLISH);

        if (normalized.contains("financial")) {
            return getFinancialAnalysis();
        }
        if (normalized.contains("contract")) {
            return getContractAnalysis();
        }
        if (normalized.contains("stakeholder")) {
            return getStakeholderAnalysis();
        }
        if (normalized.contains("billing")) {
            return getBillingAnalysis();
        }
        return getOccupancyReport();
    }

    public AIReportDTO getOccupancyReport() {
        OccupancyAnalysisDTO occupancy = getOccupancyAnalysis();
        List<Stall> stalls = stallRepository.findAll();

        Map<String, Long> stallTypeOccupancy = stalls.stream()
                .filter(stall -> isOccupied(stall.getStatus()))
                .collect(Collectors.groupingBy(
                        stall -> safeLabel(stall.getStallType(), "Uncategorized"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalStalls", occupancy.getTotalStalls());
        data.put("occupiedStalls", occupancy.getOccupiedStalls());
        data.put("vacantStalls", occupancy.getVacantStalls());
        data.put("occupancyRate", occupancy.getOccupancyRate());
        data.put("occupiedByBusinessCategory", stallTypeOccupancy);

        AIReportDTO report = baseReport("occupancy", "Occupancy Report", data);
        report.setOccupancyRate(occupancy.getOccupancyRate());
        report.setNarrative(aiText(
                "AI Report: Occupancy",
                data,
                "The market currently operates at " + formatPercent(occupancy.getOccupancyRate()) +
                        " occupancy across " + occupancy.getTotalStalls() +
                        " stall(s). " + occupancy.getVacantStalls() +
                        " stall(s) remain vacant, so reassignment activity should be prioritized where demand is available."
        ));
        report.setRiskLevel(occupancy.getOccupancyRate() < 70 ? "HIGH" : "LOW");
        report.getHighlights().add(occupancy.getOccupiedStalls() + " occupied stall(s)");
        report.getHighlights().add(occupancy.getVacantStalls() + " vacant stall(s)");
        report.getRecommendations().addAll(occupancy.getRecommendations());
        return report;
    }

    public AIReportDTO getFinancialAnalysis() {
        FinancialInsightsDTO financial = getFinancialInsights();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("expectedRevenue", financial.getTotalBilled());
        data.put("collectedRevenue", financial.getTotalCollected());
        data.put("outstandingBalance", financial.getOutstandingBalance());
        data.put("collectionEfficiency", financial.getCollectionRate());

        AIReportDTO report = baseReport("financial", "Financial Report", data);
        report.setExpectedRevenue(financial.getTotalBilled());
        report.setCollectedRevenue(financial.getTotalCollected());
        report.setOutstandingBalance(financial.getOutstandingBalance());
        report.setCollectionEfficiency(financial.getCollectionRate());
        report.setNarrative(aiText(
                "AI Report: Financial",
                data,
                "Revenue collection currently operates at " +
                        formatPercent(financial.getCollectionRate()) +
                        " efficiency. Outstanding balance is " +
                        formatMoney(financial.getOutstandingBalance()) +
                        ", so payment monitoring should remain active."
        ));
        report.setRiskLevel(financial.getCollectionRate() < 80 ? "MEDIUM" : "LOW");
        report.getHighlights().add("Expected revenue: " + formatMoney(financial.getTotalBilled()));
        report.getHighlights().add("Collected revenue: " + formatMoney(financial.getTotalCollected()));
        report.getRecommendations().addAll(financial.getRecommendations());
        return report;
    }

    public AIReportDTO getBillingAnalysis() {
        List<Billing> billings = billingRepository.findAll();
        long overdue = countOverdueBillings(billings);
        long unpaid = billings.stream()
                .filter(billing -> !"PAID".equalsIgnoreCase(safe(billing.getStatus())))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalBillings", billings.size());
        data.put("unpaidBillings", unpaid);
        data.put("overdueBillings", overdue);
        data.put("outstandingBalance", getFinancialInsights().getOutstandingBalance());

        AIReportDTO report = baseReport("billing", "Billing Report", data);
        report.setOutstandingBalance(getFinancialInsights().getOutstandingBalance());
        report.setNarrative(aiText(
                "AI Report: Billing",
                data,
                overdue + " billing account(s) are overdue and " + unpaid +
                        " billing account(s) are not marked as paid. Follow-up should prioritize overdue balances."
        ));
        report.setRiskLevel(overdue > 0 ? "MEDIUM" : "LOW");
        report.getHighlights().add(overdue + " overdue billing account(s)");
        report.getHighlights().add(unpaid + " unpaid billing account(s)");
        report.getRecommendations().add("Prioritize payment reminders for overdue billings with remaining balance.");
        return report;
    }

    public AIReportDTO getContractAnalysis() {
        List<Contract> contracts = contractRepository.findAll();
        LocalDateWindow window = new LocalDateWindow();
        long active = contracts.stream()
                .filter(contract -> "ACTIVE".equalsIgnoreCase(safe(contract.getStatus())))
                .count();
        long expiring = contracts.stream()
                .filter(contract -> "ACTIVE".equalsIgnoreCase(safe(contract.getStatus())))
                .filter(contract -> contract.getEndDate() != null)
                .filter(contract -> !contract.getEndDate().isBefore(window.today) &&
                        !contract.getEndDate().isAfter(window.next30Days))
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeContracts", active);
        data.put("expiringWithin30Days", expiring);
        data.put("totalContracts", contracts.size());

        AIReportDTO report = baseReport("contract", "Contract Report", data);
        report.setNarrative(aiText(
                "AI Report: Contract",
                data,
                expiring + " active contract(s) are projected to expire within 30 days. Early engagement with stakeholders is recommended."
        ));
        report.setRiskLevel(expiring > 0 ? "MEDIUM" : "LOW");
        report.getHighlights().add(active + " active contract(s)");
        report.getHighlights().add(expiring + " expiring contract(s) within 30 days");
        report.getRecommendations().add("Start renewal coordination before the expiration window narrows.");
        return report;
    }

    public AIReportDTO getStakeholderAnalysis() {
        List<Stakeholder> stakeholders = stakeholderRepository.findAll();
        long active = stakeholders.stream()
                .filter(stakeholder -> Boolean.TRUE.equals(stakeholder.getVerifiedTenant()))
                .count();
        long pending = stakeholders.stream()
                .filter(stakeholder -> "PENDING".equalsIgnoreCase(safe(stakeholder.getApplicationStatus())))
                .count();
        long highRisk = billingRepository.findAll()
                .stream()
                .filter(billing -> billing.getDueDate() != null)
                .filter(billing -> billing.getDueDate().isBefore(java.time.LocalDate.now()))
                .filter(billing -> zeroIfNull(billing.getBalance()).compareTo(BigDecimal.ZERO) > 0)
                .filter(billing -> billing.getOccupant() != null &&
                        billing.getOccupant().getStakeholder() != null)
                .collect(Collectors.groupingBy(
                        billing -> billing.getOccupant().getStakeholder().getId(),
                        Collectors.counting()
                ))
                .values()
                .stream()
                .filter(count -> count >= 2)
                .count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("activeStakeholders", active);
        data.put("pendingApplications", pending);
        data.put("highRiskStakeholders", highRisk);

        AIReportDTO report = baseReport("stakeholder", "Stakeholder Report", data);
        report.setNarrative(aiText(
                "AI Report: Stakeholder",
                data,
                "Most stakeholder records can be monitored through application and payment behavior. " +
                        highRisk + " stakeholder(s) show repeated overdue payment trends and require closer monitoring."
        ));
        report.setRiskLevel(highRisk > 0 ? "MEDIUM" : "LOW");
        report.getHighlights().add(active + " verified tenant stakeholder(s)");
        report.getHighlights().add(pending + " pending application(s)");
        report.getRecommendations().add("Monitor stakeholders with repeated overdue balances before renewal decisions.");
        return report;
    }

    public PaymentAnalysisDTO getPaymentAnalysis() {
        List<Payment> payments = paymentRepository.findAll();
        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        BigDecimal current = sumPaymentsForMonth(payments, currentMonth);
        BigDecimal previous = sumPaymentsForMonth(payments, previousMonth);
        double change = calculatePercentChange(current, previous);

        PaymentAnalysisDTO analysis = new PaymentAnalysisDTO();
        analysis.setCurrentMonthCollected(current);
        analysis.setPreviousMonthCollected(previous);
        analysis.setPercentageChange(change);
        analysis.setMonthlyTrends(buildMonthlyTrends(payments));

        String direction = change >= 0 ? "increased" : "decreased";
        analysis.setSummary(
                "Payments " + direction + " by " +
                        formatPercent(Math.abs(change)) +
                        " compared with last month."
        );

        if (change < 0) {
            analysis.getRecommendations().add(
                    "Review unpaid billing accounts and send reminders before the next due date cycle."
            );
        } else {
            analysis.getRecommendations().add(
                    "Maintain the current collection cadence and monitor overdue accounts weekly."
            );
        }

        return analysis;
    }

    public OccupancyAnalysisDTO getOccupancyAnalysis() {
        List<Stall> stalls = stallRepository.findAll();
        long total = stalls.size();
        long occupied = stalls.stream()
                .filter(stall -> isOccupied(stall.getStatus()))
                .count();
        long vacant = Math.max(total - occupied, 0);
        double rate = total == 0 ? 0 : roundPercent((occupied * 100.0) / total);

        OccupancyAnalysisDTO analysis = new OccupancyAnalysisDTO();
        analysis.setTotalStalls(total);
        analysis.setOccupiedStalls(occupied);
        analysis.setVacantStalls(vacant);
        analysis.setOccupancyRate(rate);
        analysis.setSummary(
                "Occupancy rate is " + formatPercent(rate) +
                        " across " + total + " stall(s)."
        );

        if (vacant > 0) {
            analysis.getRecommendations().add(
                    "Prioritize verified applicants for " + vacant + " vacant stall(s)."
            );
        } else {
            analysis.getRecommendations().add(
                    "All tracked stalls are occupied; prepare renewal monitoring for active tenants."
            );
        }

        return analysis;
    }

    public FinancialInsightsDTO getFinancialInsights() {
        List<Billing> billings = billingRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        BigDecimal totalBilled = billings.stream()
                .map(Billing::getTotalAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCollected = payments.stream()
                .map(Payment::getAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = billings.stream()
                .map(Billing::getBalance)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double collectionRate = totalBilled.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : roundPercent(totalCollected
                .multiply(BigDecimal.valueOf(100))
                .divide(totalBilled, 2, RoundingMode.HALF_UP)
                .doubleValue());

        long overdue = countOverdueBillings(billings);

        FinancialInsightsDTO insights = new FinancialInsightsDTO();
        insights.setTotalBilled(totalBilled);
        insights.setTotalCollected(totalCollected);
        insights.setOutstandingBalance(outstanding);
        insights.setCollectionRate(collectionRate);
        insights.setSummary(
                "The system collected " + formatMoney(totalCollected) +
                        " from " + formatMoney(totalBilled) +
                        " total billed amount."
        );

        insights.getInsights().add(new AIInsightDTO(
                "Collection rate",
                "Collection efficiency is " + formatPercent(collectionRate) + ".",
                collectionRate >= 85 ? "success" : "warning"
        ));
        insights.getInsights().add(new AIInsightDTO(
                "Outstanding balance",
                formatMoney(outstanding) + " remains outstanding.",
                outstanding.compareTo(BigDecimal.ZERO) > 0 ? "warning" : "success"
        ));
        insights.getInsights().add(new AIInsightDTO(
                "Overdue risk",
                overdue + " tenant billing account(s) are past due.",
                overdue > 0 ? "danger" : "success"
        ));

        if (overdue > 0) {
            insights.getRecommendations().add(
                    "Send payment follow-ups to overdue tenants and review high-balance accounts first."
            );
        }
        if (collectionRate < 85 && totalBilled.compareTo(BigDecimal.ZERO) > 0) {
            insights.getRecommendations().add(
                    "Investigate collection gaps because collected payments are below the target rate."
            );
        }
        if (insights.getRecommendations().isEmpty()) {
            insights.getRecommendations().add(
                    "Collections look stable; continue monitoring due dates and monthly movement."
            );
        }

        return insights;
    }

    private List<AITrendDTO> buildMonthlyTrends(List<Payment> payments) {
        Map<YearMonth, BigDecimal> totals = new TreeMap<>();

        for (Payment payment : payments) {
            if (payment.getPaymentDate() == null) {
                continue;
            }

            YearMonth month = YearMonth.from(payment.getPaymentDate());
            totals.merge(month, zeroIfNull(payment.getAmount()), BigDecimal::add);
        }

        return totals.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .limit(6)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new AITrendDTO(
                        entry.getKey().getMonth().getDisplayName(
                                TextStyle.SHORT,
                                Locale.ENGLISH
                        ) + " " + entry.getKey().getYear(),
                        entry.getValue()
                ))
                .toList();
    }

    private BigDecimal sumPaymentsForMonth(
            List<Payment> payments,
            YearMonth month
    ) {
        return payments.stream()
                .filter(payment -> payment.getPaymentDate() != null)
                .filter(payment -> YearMonth.from(payment.getPaymentDate()).equals(month))
                .map(Payment::getAmount)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countOverdueBillings(List<Billing> billings) {
        return billings.stream()
                .filter(billing -> billing.getDueDate() != null)
                .filter(billing -> billing.getDueDate().isBefore(java.time.LocalDate.now()))
                .filter(billing -> zeroIfNull(billing.getBalance()).compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    private double calculatePercentChange(
            BigDecimal current,
            BigDecimal previous
    ) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }

        return roundPercent(current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP)
                .doubleValue());
    }

    private boolean isOccupied(String status) {
        return "OCCUPIED".equalsIgnoreCase(safe(status)) ||
                "ACTIVE".equalsIgnoreCase(safe(status));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String formatMoney(BigDecimal value) {
        return "PHP " + zeroIfNull(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatPercent(double value) {
        return roundPercent(value) + "%";
    }

    private double roundPercent(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private AIReportDTO baseReport(
            String reportType,
            String title,
            Map<String, Object> data
    ) {
        AIReportDTO report = new AIReportDTO();
        report.setReportType(reportType);
        report.setTitle(title);
        report.setData(data);
        return report;
    }

    private String aiText(String moduleType, Map<String, Object> data, String fallback) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback;
        }

        try {
            String prompt = """
                    You are an intelligent Generative AI assistant for a Public Market Stall Management System.

                    Analyze the provided structured market data and generate professional administrative insights.

                    Requirements:
                    - professional tone
                    - concise but meaningful
                    - explain risks
                    - mention opportunities
                    - provide recommendation
                    - do not invent missing information
                    - only use provided data

                    Module Type:
                    %s

                    Market Data:
                    %s

                    Generate report or notification.
                    """.formatted(moduleType, objectMapper.writeValueAsString(data));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.2
            );

            String response = restTemplate.postForObject(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(request, headers),
                    String.class
            );

            JsonNode root = objectMapper.readTree(response);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            return content == null || content.isBlank() ? fallback : content.trim();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private String safeLabel(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value;
    }

    private static class LocalDateWindow {
        private final LocalDate today = LocalDate.now();
        private final LocalDate next30Days = today.plusDays(30);
    }
}
