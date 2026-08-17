package com.code.back_end.controller;

import com.code.back_end.dto.ApplicantFeeRequest;
import com.code.back_end.dto.RequirementStatusResponse;
import com.code.back_end.dto.StallAssignmentRequest;
import com.code.back_end.dto.TreasurerApprovalRequest;
import com.code.back_end.entity.Stakeholder;
import com.code.back_end.service.ApplicantApprovalService;
import com.code.back_end.service.ApprovalWorkflowService;
import com.code.back_end.service.StakeholderDocumentService;
import com.code.back_end.service.StakeholderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stakeholders")
public class StakeholderController {

    private final StakeholderService stakeholderService;
    private final ApplicantApprovalService applicantApprovalService;
    private final StakeholderDocumentService stakeholderDocumentService;
    private final ApprovalWorkflowService approvalWorkflowService;

    public StakeholderController(
            StakeholderService stakeholderService,
            ApplicantApprovalService applicantApprovalService,
            StakeholderDocumentService stakeholderDocumentService,
            ApprovalWorkflowService approvalWorkflowService
    ) {
        this.stakeholderService = stakeholderService;
        this.applicantApprovalService = applicantApprovalService;
        this.stakeholderDocumentService = stakeholderDocumentService;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    // =========================
    // CREATE (DEPRECATED - USE /api/applications)
    // =========================
    @PostMapping(consumes = "multipart/form-data")
    public String create(
            @RequestParam Long userId,
            @RequestParam String businessName,
            @RequestParam String businessType,
            @RequestParam String firstName,
            @RequestParam(required = false) String middleName,
            @RequestParam String lastName,
            @RequestParam String contact,
            @RequestParam String email,
            @RequestParam String address,
            @RequestParam MultipartFile idFile,
            @RequestParam MultipartFile letterFile
    ) throws IOException {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Submit business applications through /api/applications. Stakeholders are created only after approval."
        );
    }

    // =========================
    // GET ALL & BY ID
    // =========================
    @GetMapping
    public List<Stakeholder> getAll() {
        return stakeholderService.getAll();
    }

    @GetMapping("/for-approval")
    public List<Stakeholder> getForApproval() {
        return stakeholderService.getForApproval();
    }

    @GetMapping("/user/{userId}")
    public Stakeholder getByUserId(@PathVariable Long userId) {
        return stakeholderService.getByUserId(userId);
    }

    @GetMapping("/{id}")
    public Stakeholder getById(@PathVariable Long id) {
        return stakeholderService.getById(id);
    }

    // =========================
    // MARKET SUPERVISOR APPROVAL
    // =========================
    @PutMapping("/{id}/market-approve")
    public Stakeholder marketApprove(@PathVariable Long id) {
        return applicantApprovalService.approveMarketSupervisor(id);
    }

    @PutMapping("/{id}/market-reject")
    public Stakeholder marketReject(@PathVariable Long id) {
        return applicantApprovalService.rejectMarketSupervisor(id);
    }

    // =========================
    // BPLO APPROVAL
    // =========================
    @PutMapping("/{id}/bplo-approve")
    public Stakeholder bploApprove(@PathVariable Long id) {
        return applicantApprovalService.approveBplo(id);
    }

    @PutMapping("/{id}/bplo-reject")
    public Stakeholder bploReject(@PathVariable Long id) {
        return applicantApprovalService.rejectBplo(id);
    }

    // =========================
    // ENDORSEMENT OFFICE APPROVAL
    // =========================
    @PutMapping("/{id}/endorse")
    public Stakeholder endorse(@PathVariable Long id) {
        return applicantApprovalService.approveEndorsing(id);
    }

    @PutMapping("/{id}/endorse-reject")
    public Stakeholder endorseReject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return applicantApprovalService.rejectEndorsing(id, remarks);
    }

    // =========================
    // APPLICANT FEE & FINAL APPROVAL
    // =========================
    @PutMapping("/{id}/pay-applicant-fee")
    public Stakeholder payApplicantFee(
            @PathVariable Long id,
            @RequestParam BigDecimal amount
    ) {
        return applicantApprovalService.payApplicantFee(id, amount);
    }

    @PutMapping("/{id}/approve")
    public Stakeholder approve(@PathVariable Long id) {
        return applicantApprovalService.approve(id);
    }

    @PutMapping("/{id}/reject")
    public Stakeholder reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {
        return applicantApprovalService.reject(id, remarks);
    }

    // =========================
    // WORKFLOW SERVICES
    // =========================
    @PostMapping("/{id}/treasurer-approve")
    public Stakeholder treasurerApprove(
            @PathVariable Long id,
            @RequestBody TreasurerApprovalRequest request
    ) {
        return approvalWorkflowService.approveByTreasurer(id, request);
    }

    @PostMapping("/{id}/assign-stall")
    public Stakeholder assignStall(
            @PathVariable Long id,
            @RequestBody StallAssignmentRequest request
    ) {
        return approvalWorkflowService.assignStallAndCreateContract(id, request);
    }

    @PostMapping("/{id}/bplo-approve-workflow")
    public Stakeholder approveBploWorkflow(@PathVariable Long id) {
        return approvalWorkflowService.approveBplo(id);
    }

    @PostMapping("/{id}/final-endorse")
    public Stakeholder finalEndorse(@PathVariable Long id) {
        return approvalWorkflowService.finalEndorse(id);
    }

    @PostMapping("/{id}/applicant-fee")
    public Stakeholder collectApplicantFee(
            @PathVariable Long id,
            @RequestBody ApplicantFeeRequest request
    ) {
        return approvalWorkflowService.collectApplicantFee(id, request);
    }

    @GetMapping("/{id}/requirements")
    public RequirementStatusResponse requirements(@PathVariable Long id) {
        return approvalWorkflowService.getRequirementStatus(id);
    }

    // =========================
    // DELETE
    // =========================
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        return stakeholderService.delete(id);
    }

    // =========================
    // UPLOAD DOCUMENT
    // =========================
    @PutMapping(
            value = "/{userId}/upload/{type}",
            consumes = "multipart/form-data"
    )
    public Stakeholder uploadDocument(
            @PathVariable Long userId,
            @PathVariable String type,
            @RequestParam MultipartFile file
    ) throws IOException {
        Stakeholder stakeholder = stakeholderDocumentService.uploadDocument(userId, type, file);
        applicantApprovalService.updateOverallStatus(stakeholder);
        return stakeholder;
    }
}
