package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.DailyAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface DailyAnalysisRepository extends JpaRepository<DailyAnalysisEntity, LocalDate> {
}