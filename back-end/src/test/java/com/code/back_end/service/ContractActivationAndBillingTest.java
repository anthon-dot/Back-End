package com.code.back_end.service;

import com.code.back_end.entity.*;
import com.code.back_end.repository.*;
import com.code.back_end.security.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ContractActivationAndBillingTest {

    @Mock private ContractRepository contractRepository;
    @Mock private OccupantRepository occupantRepository;
    @Mock private StallRepository stallRepository;
    @Mock private StakeholderRepository stakeholderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private BusinessApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private BillingRepository billingRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;
    @Mock private SecurityService securityService;
    @Mock private StakeholderService stakeholderService;

    private BillingService billingService;
    private ContractService contractService;
    private PaymentService paymentService;

    private Stakeholder stakeholder;
    private Stall stall;
    private Occupant occupant;
    private Contract contract;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(
                billingRepository,
                contractRepository,
                occupantRepository,
                notificationService,
                stakeholderRepository,
                securityService,
                auditLogService
        );

        contractService = new ContractService(
                contractRepository,
                stallRepository,
                occupantRepository,
                billingService,
                stakeholderService,
                securityService,
                auditLogService,
                paymentRepository,
                stakeholderRepository,
                applicationRepository,
                userRepository,
                notificationService
        );

        paymentService = new PaymentService(
                paymentRepository,
                billingRepository,
                stakeholderRepository,
                notificationService,
                securityService,
                auditLogService,
                applicationRepository,
                contractRepository,
                occupantRepository,
                billingService,
                contractService
        );

        // Standard test entities
        stall = new Stall();
        stall.setId(10L);
        stall.setStallNo("STALL-A1");
        stall.setStatus("ASSIGNED");
        stall.setMonthlyRent(1500.0);

        stakeholder = new Stakeholder();
        stakeholder.setId(100L);
        stakeholder.setFirstName("Juan");
        stakeholder.setLastName("Dela Cruz");
        stakeholder.setMarketSupervisorApproved(true);
        stakeholder.setMarketApprovalStatus("APPROVED");
        stakeholder.setBploApproved(true);
        stakeholder.setEndorsingApproved(true);
        stakeholder.setApplicantFeePaid(true);
        stakeholder.setApplicantFeeAmount(new BigDecimal("250.00"));
        stakeholder.setAdvanceBalance(BigDecimal.ZERO);

        occupant = new Occupant();
        occupant.setId(200L);
        occupant.setStakeholder(stakeholder);
        occupant.setStall(stall);
        occupant.setStatus("PENDING");
        occupant.setAdvanceBalance(BigDecimal.ZERO);

        stall.setOccupant(occupant);

        contract = new Contract();
        contract.setId(300L);
        contract.setContractNo("CNT-2026-001");
        contract.setOccupant(occupant);
        contract.setStall(stall);
        contract.setStartDate(LocalDate.now());
        contract.setEndDate(LocalDate.now().plusMonths(12));
        contract.setMonthlyRent(new BigDecimal("1500.00"));
        contract.setBillingFrequency("MONTHLY");
        contract.setStatus("PENDING");

        // Repositories default mock wiring
        when(contractRepository.findById(300L)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> i.getArgument(0));
        when(occupantRepository.findById(200L)).thenReturn(Optional.of(occupant));
        when(occupantRepository.save(any(Occupant.class))).thenAnswer(i -> i.getArgument(0));
        when(stakeholderRepository.findById(100L)).thenReturn(Optional.of(stakeholder));
        when(stakeholderRepository.save(any(Stakeholder.class))).thenAnswer(i -> i.getArgument(0));
        when(billingRepository.save(any(Billing.class))).thenAnswer(i -> i.getArgument(0));
        when(stallRepository.findById(10L)).thenReturn(Optional.of(stall));
        when(stallRepository.save(any(Stall.class))).thenAnswer(i -> i.getArgument(0));

        Payment mockFeePayment = new Payment();
        mockFeePayment.setId(88L);
        mockFeePayment.setStakeholder(stakeholder);
        mockFeePayment.setAmount(new BigDecimal("250.00"));
        mockFeePayment.setPaymentType(PaymentType.APPLICATION_FEE);
        when(paymentRepository.findByStakeholderId(100L)).thenReturn(List.of(mockFeePayment));
    }

    @Test
    @DisplayName("Scenario A: Valid fee payment activates an eligible contract and occupant")
    void testValidFeePaymentActivatesEligibleContract() {
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());

        boolean activated = contractService.activateContractIfEligible(contract);

        assertTrue(activated, "Eligible contract must be activated");
        assertEquals("ACTIVE", contract.getStatus(), "Contract status must become ACTIVE");
        assertEquals("ACTIVE", occupant.getStatus(), "Occupant status must become ACTIVE");
        assertTrue(stakeholder.getVerifiedTenant(), "Stakeholder must be marked as verified tenant");
        assertEquals("APPROVED", stakeholder.getFinalStatus(), "Final status must be APPROVED");
        assertEquals("COMPLETED", stakeholder.getApplicationStatus());

        verify(contractRepository).save(contract);
        verify(occupantRepository, atLeastOnce()).save(occupant);
        verify(stakeholderRepository, atLeastOnce()).save(stakeholder);
        verify(auditLogService).log(eq("CONTRACT_ACTIVATED"), eq("Contract"), eq(300L), anyString());
    }

    @Test
    @DisplayName("Scenario A2: Initial billing is generated automatically on contract activation")
    void testValidFeePaymentGeneratesInitialBilling() {
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());

        contractService.activateContractIfEligible(contract);

        ArgumentCaptor<Billing> billingCaptor = ArgumentCaptor.forClass(Billing.class);
        verify(billingRepository, atLeastOnce()).save(billingCaptor.capture());

        Billing generated = billingCaptor.getValue();
        assertNotNull(generated, "Billing must be generated");
        assertEquals(contract.getId(), generated.getContract().getId());
        assertEquals(occupant.getId(), generated.getOccupant().getId());
        assertEquals(new BigDecimal("1500.00"), generated.getTotalAmount());
        assertEquals(new BigDecimal("1500.00"), generated.getBalance());
        assertEquals("UNPAID", generated.getStatus());
        assertNotNull(generated.getBillingNo());
        assertNotNull(generated.getBillingPeriod());
        assertNotNull(generated.getDueDate());
    }

    @Test
    @DisplayName("Scenario B: Payment before contract creation activates contract later when created")
    void testPaymentBeforeContractCreationActivatesLaterWhenContractCreated() {
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());

        Contract newContract = new Contract();
        newContract.setOccupant(occupant);
        newContract.setStall(stall);
        newContract.setStartDate(LocalDate.now());
        newContract.setEndDate(LocalDate.now().plusMonths(12));
        newContract.setMonthlyRent(new BigDecimal("1500.00"));
        newContract.setBillingFrequency("MONTHLY");

        when(contractRepository.save(any(Contract.class))).thenAnswer(i -> {
            Contract c = i.getArgument(0);
            if (c.getId() == null) c.setId(400L);
            return c;
        });

        Contract created = contractService.createContract(newContract);

        assertEquals("ACTIVE", created.getStatus(), "Contract should become ACTIVE immediately upon creation if already eligible");
        assertEquals("ACTIVE", occupant.getStatus(), "Occupant should become ACTIVE");
        verify(billingRepository, atLeastOnce()).save(any(Billing.class));
    }

    @Test
    @DisplayName("Scenario C: Payment before approvals complete does not prematurely activate contract")
    void testPaymentBeforeFinalApprovalDoesNotPrematurelyActivate() {
        // Stakeholder has pending BPLO approval
        stakeholder.setBploApproved(false);

        boolean activated = contractService.activateContractIfEligible(contract);

        assertFalse(activated, "Contract must NOT activate when required approval is pending");
        assertEquals("PENDING", contract.getStatus(), "Contract must remain PENDING");
        assertEquals("PENDING", occupant.getStatus(), "Occupant must remain PENDING");
        verify(billingRepository, never()).save(any(Billing.class));
    }

    @Test
    @DisplayName("Scenario C2: Completing final approval activates eligible contract")
    void testCompletingFinalApprovalActivatesEligibleContract() {
        // Pending approval initially
        stakeholder.setBploApproved(false);
        when(contractRepository.findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(100L))
                .thenReturn(Optional.of(contract));
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());

        assertFalse(contractService.activateContractIfEligible(contract));

        // Now BPLO approves
        stakeholder.setBploApproved(true);
        boolean activatedNow = contractService.activateContractIfEligibleForStakeholder(100L);

        assertTrue(activatedNow, "Contract must activate once final required approval is granted");
        assertEquals("ACTIVE", contract.getStatus());
        assertEquals("ACTIVE", occupant.getStatus());
        verify(billingRepository, atLeastOnce()).save(any(Billing.class));
    }

    @Test
    @DisplayName("Scenario D: Already active contract is not reactivated or duplicated")
    void testAlreadyActiveContractDoesNotDuplicateInitialBilling() {
        contract.setStatus("ACTIVE");
        Billing existingBilling = new Billing();
        existingBilling.setId(999L);
        existingBilling.setContract(contract);
        existingBilling.setBillingPeriod("MONTHLY-" + LocalDate.now().getMonthValue() + "-" + LocalDate.now().getYear());

        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString()))
                .thenReturn(Optional.of(existingBilling));
        when(billingRepository.existsByContractIdAndBillingPeriod(anyLong(), anyString()))
                .thenReturn(true);

        boolean result = contractService.activateContractIfEligible(contract);

        assertTrue(result, "Already active contract returns true");
        assertEquals("ACTIVE", contract.getStatus());
        // Verify no duplicate billing was saved
        verify(billingRepository, never()).save(any(Billing.class));
    }

    @Test
    @DisplayName("Scenario E: Cancelled contract is not activated and receives no billing")
    void testCancelledContractIsNotActivated() {
        contract.setStatus("CANCELLED");

        boolean activated = contractService.activateContractIfEligible(contract);

        assertFalse(activated, "Cancelled contract must NOT be activated");
        assertEquals("CANCELLED", contract.getStatus());
        verify(billingRepository, never()).save(any(Billing.class));
    }

    @Test
    @DisplayName("Billing idempotency: Repeated activation calls do not generate duplicate billing")
    void testRepeatedActivationCallsDoNotGenerateDuplicateBilling() {
        // First call: no billing exists yet
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());
        contractService.activateContractIfEligible(contract);

        // Mock that billing now exists
        Billing savedBill = new Billing();
        savedBill.setId(555L);
        savedBill.setContract(contract);
        savedBill.setBillingPeriod("MONTHLY-" + LocalDate.now().getMonthValue() + "-" + LocalDate.now().getYear());
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.of(savedBill));
        when(billingRepository.existsByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(true);

        // Second call: idempotent check
        billingService.generateInitialBilling(contract);

        // Billing repository save should only have been called once for the first creation
        verify(billingRepository, times(1)).save(any(Billing.class));
    }

    @Test
    @DisplayName("Billing frequencies: WEEKLY, SEMI_MONTHLY, 15_DAYS, and MONTHLY calculations")
    void testBillingFrequencyCalculations() {
        contract.setStatus("ACTIVE");
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());

        // Test Weekly
        contract.setBillingFrequency("WEEKLY");
        billingService.generateInitialBilling(contract);

        ArgumentCaptor<Billing> billingCaptor = ArgumentCaptor.forClass(Billing.class);
        verify(billingRepository, atLeastOnce()).save(billingCaptor.capture());

        Billing captured = billingCaptor.getValue();
        assertNotNull(captured);
        assertTrue(captured.getBillingPeriod().startsWith("WEEKLY-"));
        assertEquals(contract.getStartDate(), captured.getDueDate());

        // Test Semi-monthly
        reset(billingRepository);
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());
        contract.setBillingFrequency("SEMI_MONTHLY");
        billingService.generateInitialBilling(contract);
        verify(billingRepository).save(billingCaptor.capture());
        assertTrue(billingCaptor.getValue().getBillingPeriod().startsWith("SEMI_MONTHLY-"));
        assertEquals(contract.getStartDate(), billingCaptor.getValue().getDueDate());

        // Test 15_DAYS
        reset(billingRepository);
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());
        contract.setBillingFrequency("15_DAYS");
        billingService.generateInitialBilling(contract);
        verify(billingRepository).save(billingCaptor.capture());
        assertTrue(billingCaptor.getValue().getBillingPeriod().startsWith("15_DAYS-"));
        assertEquals(contract.getStartDate(), billingCaptor.getValue().getDueDate());

        // Test Monthly
        reset(billingRepository);
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());
        contract.setBillingFrequency("MONTHLY");
        billingService.generateInitialBilling(contract);
        verify(billingRepository).save(billingCaptor.capture());
        assertTrue(billingCaptor.getValue().getBillingPeriod().startsWith("MONTHLY-"));
        assertEquals(contract.getStartDate(), billingCaptor.getValue().getDueDate());
    }

    @Test
    @DisplayName("Advance payment: Advance balance applied accurately without double deduction")
    void testAdvanceBalanceHandlingWithoutDoubleDeduction() {
        contract.setStatus("ACTIVE");
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.empty());

        // Stakeholder has 500 advance balance
        stakeholder.setAdvanceBalance(new BigDecimal("500.00"));
        occupant.setAdvanceBalance(new BigDecimal("500.00"));

        billingService.generateInitialBilling(contract);

        ArgumentCaptor<Billing> captor = ArgumentCaptor.forClass(Billing.class);
        verify(billingRepository).save(captor.capture());
        Billing bill = captor.getValue();

        assertNotNull(bill);
        assertEquals(new BigDecimal("1500.00"), bill.getTotalAmount());
        assertEquals(new BigDecimal("500.00"), bill.getPaidAmount(), "Advance balance should be credited as paidAmount");
        assertEquals(new BigDecimal("1000.00"), bill.getBalance(), "Balance should be total minus advance");
        assertEquals("PARTIAL", bill.getStatus());

        // Stakeholder and occupant advance balance should now be 0
        assertEquals(BigDecimal.ZERO, stakeholder.getAdvanceBalance());
        assertEquals(BigDecimal.ZERO, occupant.getAdvanceBalance());

        // Second generation call with existing billing should not deduct anything more
        when(billingRepository.findByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(Optional.of(bill));
        when(billingRepository.existsByContractIdAndBillingPeriod(anyLong(), anyString())).thenReturn(true);
        billingService.generateInitialBilling(contract);
        assertEquals(BigDecimal.ZERO, stakeholder.getAdvanceBalance());
    }

    @Test
    @DisplayName("Application Fee payment does not mark rent billing as paid")
    void testApplicationFeePaymentDoesNotMarkRentBillingAsPaid() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> {
            Payment p = i.getArgument(0);
            p.setId(777L);
            return p;
        });

        Payment appFeePayment = new Payment();
        appFeePayment.setStakeholder(stakeholder);
        appFeePayment.setAmount(new BigDecimal("250.00"));
        appFeePayment.setPaymentType(PaymentType.APPLICATION_FEE);
        appFeePayment.setReceiptNo("OR-APP-001");

        Payment saved = paymentService.createPayment(appFeePayment);

        assertNotNull(saved);
        assertEquals(PaymentType.APPLICATION_FEE, saved.getPaymentType());
        // Verify no billing was updated or marked as paid
        verify(billingRepository, never()).save(any(Billing.class));
    }
}
