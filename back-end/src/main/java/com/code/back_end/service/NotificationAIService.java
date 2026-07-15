package com.code.back_end.service;

import com.code.back_end.dto.AINotificationDTO;
import com.code.back_end.entity.Billing;
import com.code.back_end.entity.BusinessApplication;
import com.code.back_end.entity.Contract;
import com.code.back_end.entity.Notification;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.entity.Stall;
import com.code.back_end.repository.BillingRepository;
import com.code.back_end.repository.BusinessApplicationRepository;
import com.code.back_end.repository.ContractRepository;
import com.code.back_end.repository.NotificationRepository;
import com.code.back_end.repository.StallRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationAIService {

    private static final String MASTER_PROMPT = """
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
            """;

    private final BillingRepository billingRepository;
    private final ContractRepository contractRepository;
    private final StallRepository stallRepository;
    private final BusinessApplicationRepository businessApplicationRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.openai.api-key:}")
    private String apiKey;

    @Value("${ai.openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.openai.model:gpt-4o-mini}")
    private String model;

    public NotificationAIService(
            BillingRepository billingRepository,
            ContractRepository contractRepository,
            StallRepository stallRepository,
            BusinessApplicationRepository businessApplicationRepository,
            NotificationRepository notificationRepository,
            ObjectMapper objectMapper
    ) {
        this.billingRepository = billingRepository;
        this.contractRepository = contractRepository;
        this.stallRepository = stallRepository;
        this.businessApplicationRepository = businessApplicationRepository;
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
    }

    public List<AINotificationDTO> getNotifications() {
        return notificationRepository.findByAiGeneratedTrueOrderByCreatedAtDesc()
                .stream()
                .map(AINotificationDTO::new)
                .toList();
    }

    public List<AINotificationDTO> getUnreadNotifications() {
        return notificationRepository.findByAiGeneratedTrueAndIsReadFalseOrderByCreatedAtDesc()
                .stream()
                .map(AINotificationDTO::new)
                .toList();
    }

    public AINotificationDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return new AINotificationDTO(notificationRepository.save(notification));
    }

    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }

    public List<AINotificationDTO> generateNotifications() {
        List<Notification> generated = new ArrayList<>();
        generated.addAll(generateOverduePaymentAlerts());
        generated.addAll(generateContractExpirationAlerts());
        generated.addAll(generateLowOccupancyAlerts());
        generated.addAll(generateHighRiskTenantAlerts());
        generated.addAll(generateVacantStallAlerts());
        generated.addAll(generateApprovalBottleneckAlerts());

        if (generated.isEmpty()) {
            return getNotifications();
        }

        return generated.stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(AINotificationDTO::new)
                .toList();
    }

    private List<Notification> generateOverduePaymentAlerts() {
        LocalDate today = LocalDate.now();
        List<Notification> notifications = new ArrayList<>();

        for (Billing billing : billingRepository.findAll()) {
            Contract contract = billing.getContract();
            if (contract == null || contract.getStartDate() == null ||
                    contract.getEndDate() == null ||
                    balance(billing).compareTo(BigDecimal.ZERO) <= 0 ||
                    billing.getOccupant() == null ||
                    billing.getOccupant().getStakeholder() == null) {
                continue;
            }

            LocalDate expectedDueDate = contractCycleDueDate(contract, billing, today);
            if (expectedDueDate == null || !expectedDueDate.isBefore(today)) {
                continue;
            }

            long daysOverdue = ChronoUnit.DAYS.between(expectedDueDate, today);
            if (daysOverdue <= 5) {
                continue;
            }

            Stakeholder stakeholder = billing.getOccupant().getStakeholder();
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("businessName", stakeholder.getBusinessName());
            data.put("billingNo", billing.getBillingNo());
            data.put("daysOverdue", daysOverdue);
            data.put("balance", balance(billing));
            data.put("contractStartDate", contract.getStartDate());
            data.put("contractEndDate", contract.getEndDate());
            data.put("billingFrequency", contract.getBillingFrequency());
            data.put("cycleDueDate", expectedDueDate);
            data.put("billingStatus", billing.getStatus());

            String fallback = stakeholder.getBusinessName() + " has exceeded the payment deadline by " +
                    daysOverdue + " day(s) for the contract billing cycle due on " +
                    expectedDueDate + ". Immediate payment follow-up is recommended.";

            notifications.add(createOncePerDay(
                    stakeholder,
                    "Overdue Payment Alert",
                    aiText("AI Notification: Overdue Payment", data, fallback),
                    "The billing record remains unpaid more than five days after its contract-based billing cycle due date.",
                    "Send a payment reminder and review the account before escalation.",
                    "HIGH",
                    "OVERDUE_PAYMENT",
                    "BILLING",
                    billing.getId()
            ));
        }

        return notifications;
    }

   private List<Notification> generateContractExpirationAlerts() {
    LocalDate today = LocalDate.now();
    List<Notification> notifications = new ArrayList<>();

    for (Contract contract : contractRepository.findAll()) {

        // Validate contract
        if (!"ACTIVE".equalsIgnoreCase(safe(contract.getStatus())) ||
                contract.getOccupant() == null ||
                contract.getOccupant().getStakeholder() == null ||
                contract.getEndDate() == null) {
            continue;
        }

        long daysUntilExpiration =
                ChronoUnit.DAYS.between(today, contract.getEndDate());

        // Only notify contracts expiring within 30 days
        if (daysUntilExpiration < 0 || daysUntilExpiration > 30) {
            continue;
        }

        Stakeholder stakeholder =
                contract.getOccupant().getStakeholder();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("businessName", stakeholder.getBusinessName());
        data.put("contractNo", contract.getContractNo());
        data.put("contractStartDate", contract.getStartDate());
        data.put("contractEndDate", contract.getEndDate());
        data.put("daysUntilExpiration", daysUntilExpiration);

        // Dynamic priority
        String priority;
        if (daysUntilExpiration <= 7) {
            priority = "HIGH";
        } else if (daysUntilExpiration <= 15) {
            priority = "MEDIUM";
        } else {
            priority = "LOW";
        }

        String fallback =
                "Contract " +
                safeLabel(contract.getContractNo(),
                        "#" + contract.getId()) +
                " for " +
                stakeholder.getBusinessName() +
                " will expire in " +
                daysUntilExpiration +
                " day(s). Renewal coordination should begin immediately.";

        notifications.add(createOncePerDay(
                stakeholder,
                "Contract Ending Soon",
                aiText(
                        "AI Notification: Contract Expiration",
                        data,
                        fallback
                ),
                "The active contract is nearing expiration within the next 30 days.",
                "Coordinate renewal processing or prepare stall reassignment if renewal is delayed.",
                priority,
                "CONTRACT_EXPIRATION",
                "CONTRACT",
                contract.getId()
        ));
    }

    return notifications;
}

    private List<Notification> generateLowOccupancyAlerts() {
        List<Stall> stalls = stallRepository.findAll();
        long total = stalls.size();
        long occupied = stalls.stream().filter(stall -> isOccupied(stall.getStatus())).count();
        double rate = total == 0 ? 0 : round((occupied * 100.0) / total);

        if (total == 0 || rate >= 70) {
            return List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalStalls", total);
        data.put("occupiedStalls", occupied);
        data.put("vacantStalls", total - occupied);
        data.put("occupancyRate", rate);

        String fallback = "The market currently operates below the expected occupancy threshold at " +
                rate + "%. Prioritizing reassignment of vacant stalls is recommended.";

        return List.of(createOncePerDay(
                null,
                "Low Occupancy Alert",
                aiText("AI Notification: Low Occupancy", data, fallback),
                "Overall stall utilization is below the 70% management threshold.",
                "Prioritize qualified applicants and reassignment actions for vacant stalls.",
                "HIGH",
                "LOW_OCCUPANCY",
                "STALL",
                0L
        ));
    }

    private List<Notification> generateHighRiskTenantAlerts() {
        Map<Long, Long> lateCounts = new LinkedHashMap<>();

        for (Billing billing : billingRepository.findAll()) {
            if (billing.getDueDate() == null ||
                    !billing.getDueDate().isBefore(LocalDate.now()) ||
                    balance(billing).compareTo(BigDecimal.ZERO) <= 0 ||
                    billing.getOccupant() == null ||
                    billing.getOccupant().getStakeholder() == null) {
                continue;
            }

            lateCounts.merge(
                    billing.getOccupant().getStakeholder().getId(),
                    1L,
                    Long::sum
            );
        }

        long highRiskCount = lateCounts.values().stream()
                .filter(count -> count >= 2)
                .count();

        if (highRiskCount == 0) {
            return List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("highRiskTenants", highRiskCount);
        data.put("riskCondition", "two or more overdue billing cycles with outstanding balance");

        String fallback = highRiskCount +
                " tenant(s) demonstrate repeated delayed payment behavior across multiple billing cycles. Proactive monitoring is recommended.";

        return List.of(createOncePerDay(
                null,
                "High-Risk Tenant Alert",
                aiText("AI Notification: High-Risk Tenant", data, fallback),
                "Multiple overdue billing records indicate elevated collection risk.",
                "Review tenant payment history and assign proactive monitoring.",
                "HIGH",
                "HIGH_RISK_TENANT",
                "BILLING",
                0L
        ));
    }

    private List<Notification> generateVacantStallAlerts() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        List<Stall> vacant = stallRepository.findAll()
                .stream()
                .filter(stall -> !isOccupied(stall.getStatus()))
                .filter(stall -> stall.getCreatedAt() != null &&
                        stall.getCreatedAt().isBefore(threshold))
                .toList();

        if (vacant.isEmpty()) {
            return List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("vacantStallsOver30Days", vacant.size());
        data.put("stallNumbers", vacant.stream().map(Stall::getStallNo).toList());

        String fallback = vacant.size() +
                " stall(s) have remained vacant for over one month, reducing revenue opportunities. Consider reassignment campaigns.";

        return List.of(createOncePerDay(
                null,
                "Vacant Stall Alert",
                aiText("AI Notification: Vacant Stall", data, fallback),
                "Vacant stalls older than 30 days represent unrealized rental opportunity.",
                "Coordinate reassignment campaigns for eligible vacant stalls.",
                "MEDIUM",
                "VACANT_STALL",
                "STALL",
                vacant.get(0).getId()
        ));
    }

    private List<Notification> generateApprovalBottleneckAlerts() {
        LocalDate threshold = LocalDate.now().minusDays(5);
        List<BusinessApplication> delayed = businessApplicationRepository.findAll()
                .stream()
                .filter(application -> application.getAppliedOn() != null)
                .filter(application -> application.getAppliedOn().isBefore(threshold))
                .filter(application -> "PENDING".equalsIgnoreCase(safe(application.getApplicationStatus())) ||
                        "PENDING".equalsIgnoreCase(safe(application.getMarketApprovalStatus())) ||
                        "PENDING".equalsIgnoreCase(safe(application.getEndorsementStatus())) ||
                        "PENDING".equalsIgnoreCase(safe(application.getBploStatus())))
                .toList();

        if (delayed.isEmpty()) {
            return List.of();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingApplicationsOverFiveDays", delayed.size());
        data.put("oldestAppliedOn", delayed.stream()
                .map(BusinessApplication::getAppliedOn)
                .min(LocalDate::compareTo)
                .orElse(null));

        String fallback = delayed.size() +
                " application(s) have remained in pending approval stages for more than five days.";

        return List.of(createOncePerDay(
                null,
                "Approval Bottleneck Alert",
                aiText("AI Notification: Approval Bottleneck", data, fallback),
                "Pending approval stages are aging beyond the expected review window.",
                "Review pending market, endorsement, and BPLO approvals and assign follow-up owners.",
                "MEDIUM",
                "APPROVAL_BOTTLENECK",
                "APPLICATION",
                delayed.get(0).getId()
        ));
    }

    @Scheduled(cron = "${ai.notifications.overdue-schedule:0 0 6 * * *}")
    public void detectOverduePayments() {
        generateOverduePaymentAlerts();
    }

    @Scheduled(cron = "${ai.notifications.expiration-schedule:0 10 6 * * *}")
    public void detectContractExpirations() {
        generateContractExpirationAlerts();
    }

    @Scheduled(cron = "${ai.notifications.vacancy-schedule:0 20 6 * * *}")
    public void detectVacancyAlerts() {
        generateVacantStallAlerts();
        generateLowOccupancyAlerts();
    }

    @Scheduled(cron = "${ai.notifications.approval-schedule:0 30 6 * * *}")
    public void detectApprovalBottlenecks() {
        generateApprovalBottleneckAlerts();
    }

    private Notification createOncePerDay(
            Stakeholder stakeholder,
            String title,
            String message,
            String explanation,
            String recommendation,
            String priority,
            String type,
            String relatedType,
            Long relatedId
    ) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        boolean exists = notificationRepository
                .existsByAiGeneratedTrueAndNotificationTypeAndRelatedRecordIdAndCreatedAtBetween(
                        type,
                        relatedId,
                        start,
                        end
                );

        if (exists) {
            return notificationRepository.findByAiGeneratedTrueOrderByCreatedAtDesc()
                    .stream()
                    .filter(item -> type.equals(item.getNotificationType()))
                    .filter(item -> relatedId.equals(item.getRelatedRecordId()))
                    .findFirst()
                    .orElseThrow();
        }

        Notification notification = new Notification();
        notification.setStakeholder(stakeholder);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setExplanation(explanation);
        notification.setRecommendation(recommendation);
        notification.setPriority(priority);
        notification.setNotificationType(type);
        notification.setRelatedRecordType(relatedType);
        notification.setRelatedRecordId(relatedId);
        notification.setAiGenerated(true);
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }

    private String aiText(String moduleType, Map<String, Object> data, String fallback) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallback;
        }

        try {
            String marketData = objectMapper.writeValueAsString(data);
            String prompt = MASTER_PROMPT.formatted(moduleType, marketData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
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

    private BigDecimal balance(Billing billing) {
        return billing.getBalance() == null ? BigDecimal.ZERO : billing.getBalance();
    }

    private boolean isOccupied(String status) {
        return "OCCUPIED".equalsIgnoreCase(safe(status)) ||
                "ACTIVE".equalsIgnoreCase(safe(status));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeLabel(String value, String fallback) {
        return safe(value).isBlank() ? fallback : value;
    }

    private LocalDate contractCycleDueDate(Contract contract, Billing billing, LocalDate today) {
        LocalDate start = contract.getStartDate();
        LocalDate end = contract.getEndDate();

        if (today.isBefore(start) || today.isAfter(end)) {
            return null;
        }

        int dueDay = billing.getDueDate() == null
                ? start.getDayOfMonth()
                : billing.getDueDate().getDayOfMonth();
        int monthStep = billingMonthStep(contract.getBillingFrequency());
        LocalDate cycleStart = start;
        LocalDate dueDate = withDueDay(cycleStart, dueDay);

        while (dueDate.isBefore(start)) {
            cycleStart = cycleStart.plusMonths(monthStep);
            dueDate = withDueDay(cycleStart, dueDay);
        }

        while (dueDate.isAfter(today) && cycleStart.isAfter(start)) {
            cycleStart = cycleStart.minusMonths(monthStep);
            dueDate = withDueDay(cycleStart, dueDay);
        }

        while (!dueDate.plusMonths(monthStep).isAfter(today)) {
            cycleStart = cycleStart.plusMonths(monthStep);
            dueDate = withDueDay(cycleStart, dueDay);
        }

        return dueDate.isAfter(end) ? end : dueDate;
    }

    private int billingMonthStep(String billingFrequency) {
        String normalized = safe(billingFrequency).toUpperCase();
        if (normalized.contains("ANNUAL") || normalized.contains("YEAR")) {
            return 12;
        }
        if (normalized.contains("QUARTER")) {
            return 3;
        }
        if (normalized.contains("SEMI")) {
            return 6;
        }
        return 1;
    }

    private LocalDate withDueDay(LocalDate month, int dueDay) {
        int day = Math.min(Math.max(dueDay, 1), month.lengthOfMonth());
        return LocalDate.of(month.getYear(), month.getMonth(), day);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
