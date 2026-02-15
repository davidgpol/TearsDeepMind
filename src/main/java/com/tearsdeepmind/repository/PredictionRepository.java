package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.PredictionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PredictionRepository extends JpaRepository<PredictionEntity, UUID> {
    List<PredictionEntity> findByReportDate(LocalDate reportDate);
}
