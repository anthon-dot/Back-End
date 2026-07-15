package com.code.back_end.repository;

import com.code.back_end.entity.StakeholderDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StakeholderDocumentRepository
        extends JpaRepository<StakeholderDocument, Long> {

    List<StakeholderDocument> findByStakeholder_Id(Long stakeholderId);
}