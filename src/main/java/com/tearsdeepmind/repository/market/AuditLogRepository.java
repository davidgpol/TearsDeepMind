package com.tearsdeepmind.repository.market;

import com.tearsdeepmind.entity.market.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    Optional<AuditLogEntity> findByDate(LocalDate date);
}
