package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, LocalDate> {
}
