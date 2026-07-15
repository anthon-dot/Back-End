package com.code.back_end.repository;

import com.code.back_end.entity.ApprovalHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalHistoryRepository
        extends JpaRepository<ApprovalHistory, Long> {

    List<ApprovalHistory> findByStakeholder_IdOrderByCreatedAtAsc(
            Long stakeholderId
    );
}
