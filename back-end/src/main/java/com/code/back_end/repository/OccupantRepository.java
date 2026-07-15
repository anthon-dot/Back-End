package com.code.back_end.repository;

import com.code.back_end.entity.Occupant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OccupantRepository
        extends JpaRepository<
                Occupant,
                Long
                > {

    boolean existsByStakeholder_Id(
            Long stakeholderId
    );

    Optional<Occupant> findByStakeholder_IdAndIsArchivedFalse(
            Long stakeholderId
    );

    boolean existsByStall_IdAndIsArchivedFalse(
            Long stallId
    );
}
