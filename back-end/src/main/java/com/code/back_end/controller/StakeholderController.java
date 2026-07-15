package com.code.back_end.controller;

import com.code.back_end.entity.Stakeholder;
import com.code.back_end.dto.ApplicantFeeRequest;
import com.code.back_end.dto.RequirementStatusResponse;
import com.code.back_end.dto.StallAssignmentRequest;
import com.code.back_end.dto.TreasurerApprovalRequest;
import com.code.back_end.service.ApprovalWorkflowService;
import com.code.back_end.service.StakeholderService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stakeholders")
public class StakeholderController {

    private final StakeholderService service;
    private final ApprovalWorkflowService approvalWorkflowService;

    public StakeholderController(
            StakeholderService service,
            ApprovalWorkflowService approvalWorkflowService
    ) {

        this.service = service;
        this.approvalWorkflowService = approvalWorkflowService;
    }

    // =========================
    // CREATE
    // =========================

    @PostMapping(
            consumes = "multipart/form-data"
    )
    public String create(

            @RequestParam Long userId,

            @RequestParam String businessName,

            @RequestParam String businessType,

            @RequestParam String firstName,

            @RequestParam(required = false)
            String middleName,

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
    // GET ALL
    // =========================

    @GetMapping
    public List<Stakeholder> getAll() {

        return service.getAll();
    }

    @GetMapping("/for-approval")
    public List<Stakeholder> getForApproval() {

        return service.getForApproval();
    }

    // =========================
    // GET BY ID
    // =========================
// =========================
// GET BY USER ID
// =========================
@GetMapping("/user/{userId}")
public Stakeholder getByUserId(
        @PathVariable Long userId
) {

    return service.getByUserId(userId);
}

// GET BY ID
@GetMapping("/{id}")
public Stakeholder getById(
        @PathVariable Long id
) {
    return service.getById(id);
}

    // =========================
    // MARKET SUPERVISOR APPROVE
    // =========================

    @PutMapping("/{id}/market-supervisor")
    public Stakeholder marketSupervisorApprove(
            @PathVariable Long id
    ) {

        return service
                .approveMarketSupervisor(id);
    }

    @PutMapping("/{id}/market-approve")
    public Stakeholder marketApprove(
            @PathVariable Long id
    ) {

        return service
                .approveMarketSupervisor(id);
    }

    @PutMapping("/{id}/market-reject")
    public Stakeholder marketReject(
            @PathVariable Long id
    ) {

        return service
                .rejectMarketSupervisor(id);
    }

    // =========================
    // BPLO APPROVE
    // =========================

    @PutMapping("/{id}/bplo")
    public Stakeholder bploApprove(
            @PathVariable Long id
    ) {

        return service
                .approveBplo(id);
    }

    @PutMapping("/{id}/bplo-approve")
    public Stakeholder bploApproveStage(
            @PathVariable Long id
    ) {

        return service
                .approveBplo(id);
    }

    @PutMapping("/{id}/bplo-reject")
    public Stakeholder bploReject(
            @PathVariable Long id
    ) {

        return service
                .rejectBplo(id);
    }

    // =========================
    // ENDORSING APPROVE
    // =========================

    @PutMapping("/{id}/endorsing")
    public Stakeholder endorsingApprove(
            @PathVariable Long id
    ) {

        return service
                .approveEndorsing(id);
    }

    @PutMapping("/{id}/endorse")
    public Stakeholder endorse(
            @PathVariable Long id
    ) {

        return service
                .approveEndorsing(id);
    }

    @PutMapping("/{id}/endorse-reject")
    public Stakeholder endorseReject(
            @PathVariable Long id,

            @RequestParam(required = false)
            String remarks
    ) {

        return service
                .rejectEndorsing(id, remarks);
    }

    @PutMapping("/{id}/pay-applicant-fee")
    public Stakeholder payApplicantFee(
            @PathVariable Long id,

            @RequestParam BigDecimal amount
    ) {

        return service
                .payApplicantFee(id, amount);
    }

    // =========================
    // FINAL APPROVE
    // =========================

    @PutMapping("/{id}/approve")
    public Stakeholder approve(
            @PathVariable Long id
    ) {

        return service.approve(id);
    }

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
    public Stakeholder approveBploWorkflow(
            @PathVariable Long id
    ) {

        return approvalWorkflowService.approveBplo(id);
    }

    @PostMapping("/{id}/final-endorse")
    public Stakeholder finalEndorse(
            @PathVariable Long id
    ) {

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
    public RequirementStatusResponse requirements(
            @PathVariable Long id
    ) {

        return approvalWorkflowService.getRequirementStatus(id);
    }

    // =========================
    // REJECT
    // =========================

    @PutMapping("/{id}/reject")
    public Stakeholder reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks
    ) {

        return service.reject(id, remarks);
    }

    // =========================
    // DELETE
    // =========================

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long id
    ) {

        return service.delete(id);      
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

    return service.uploadDocument(
            userId,
            type,
            file
    );
}
}
