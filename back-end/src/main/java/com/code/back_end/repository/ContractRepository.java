// =======================================
// ContractRepository.java
// =======================================
package com.code.back_end.repository;

import com.code.back_end.entity.Contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository
        extends JpaRepository<Contract, Long> {

    List<Contract> findByStallId(Long stallId);

    List<Contract> findByOccupant_Stakeholder_User_Id(
            Long userId
    );

    Optional<Contract> findByIdAndOccupant_Stakeholder_User_Id(
            Long id,
            Long userId
    );

    boolean existsByOccupant_Stakeholder_Id(
            Long stakeholderId
    );

    Optional<Contract> findFirstByOccupant_Stakeholder_IdOrderByCreatedAtDesc(
            Long stakeholderId
    );
}
