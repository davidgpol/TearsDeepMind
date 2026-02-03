package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.RawDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RawDocumentRepository extends JpaRepository<RawDocumentEntity, UUID> {
    Optional<RawDocumentEntity> findByDateAndType(LocalDate date, String type);
}
