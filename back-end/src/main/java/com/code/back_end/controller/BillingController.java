// =======================================
// BillingController.java
// =======================================
package com.code.back_end.controller;

import com.code.back_end.dto.BillingDTO;
import com.code.back_end.entity.Billing;
import com.code.back_end.service.BillingService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billings")
public class BillingController {

    private final BillingService service;

    public BillingController(
            BillingService service
    ) {

        this.service = service;
    }

    // =====================================
    // GET ALL BILLINGS
    // =====================================
    @GetMapping
    public List<BillingDTO>
    getAllBillings() {

        return service
                .getAllBillingDTOs();
    }

    // =====================================
    // GET BILLINGS BY STAKEHOLDER
    // =====================================
    @GetMapping("/stakeholder/{id}")
    public List<BillingDTO>
    getBillingByStakeholder(

            @PathVariable Long id
    ) {

        return service
                .getBillingByStakeholder(
                        id
                );
    }

    // =====================================
    // GET BILLING BY ID
    // =====================================
    @GetMapping("/{id}")
    public ResponseEntity<Billing>
    getBillingById(

            @PathVariable Long id
    ) {

        return service.getBillingById(id)

                .map(ResponseEntity::ok)

                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =====================================
    // DELETE BILLING
    // =====================================
    @DeleteMapping("/{id}")
    public ResponseEntity<String>
    deleteBilling(

            @PathVariable Long id
    ) {

        service.deleteBilling(
                id
        );

        return ResponseEntity.ok(
                "Billing deleted successfully"
        );
    }
@PostMapping("/notify/{billingNo}")
public ResponseEntity<String> sendBillingNotification(
        @PathVariable String billingNo
) {

   Billing billing =
        service.getByBillingNo(billingNo);

   service.sendBillingNotification(
        billing
   );

   return ResponseEntity.ok("Notification sent");
}
}