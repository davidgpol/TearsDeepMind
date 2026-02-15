package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.QuantSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuantSnapshotRepository extends JpaRepository<QuantSnapshotEntity, UUID> {
    List<QuantSnapshotEntity> findByReportDate(LocalDate reportDate);
}
