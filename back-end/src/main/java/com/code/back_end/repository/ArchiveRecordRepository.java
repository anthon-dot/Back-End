package com.code.back_end.repository;

import com.code.back_end.entity.ArchiveRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchiveRecordRepository
        extends JpaRepository<ArchiveRecord, Long> {
}