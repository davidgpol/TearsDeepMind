package com.tearsdeepmind.repository;

import com.tearsdeepmind.domain.model.DailyAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyAnalysisRepository extends JpaRepository<DailyAnalysisEntity, String> {
}
