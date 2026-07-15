// =======================================
// ContractController.java
// =======================================
package com.code.back_end.controller;

import com.code.back_end.entity.Contract;
import com.code.back_end.service.ContractService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    // =========================
    // GET ALL CONTRACTS
    // =========================
    @GetMapping
    public List<Contract> getAllContracts() {
        return contractService.getAllContracts();
    }

    // =========================
    // GET CONTRACT BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<Contract> getContractById(
            @PathVariable Long id
    ) {

        return contractService.getContractById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // CREATE CONTRACT
    // =========================
    @PostMapping
    public ResponseEntity<Contract> createContract(
            @Valid @RequestBody Contract contract
    ) {

        Contract savedContract =
                contractService.createContract(contract);

        return ResponseEntity.ok(savedContract);
    }

    // =========================
    // UPDATE CONTRACT
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<Contract> updateContract(
            @PathVariable Long id,
            @Valid @RequestBody Contract contract
    ) {

        Contract updatedContract =
                contractService.updateContract(id, contract);

        return ResponseEntity.ok(updatedContract);
    }

    // =========================
    // DELETE CONTRACT
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteContract(
            @PathVariable Long id
    ) {

        contractService.deleteContract(id);

        return ResponseEntity.ok(
                "Contract deleted successfully"
        );
    }
    // =========================
// UPDATE CONTRACT STATUS
// =========================
@PatchMapping("/{id}/status")
public ResponseEntity<?> updateStatus(
        @PathVariable Long id,
        @RequestParam String status
) {

    try {

        Contract updated =
                contractService.updateContractStatus(
                        id,
                        status
                );

        return ResponseEntity.ok(updated);

    } catch (RuntimeException e) {

        return ResponseEntity.badRequest()
                .body(e.getMessage());
    }
}
}
