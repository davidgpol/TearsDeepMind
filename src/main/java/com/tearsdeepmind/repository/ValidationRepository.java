package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.ValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ValidationRepository extends JpaRepository<ValidationEntity, UUID> {
    List<ValidationEntity> findTop3ByNotesNotOrderByCreatedAtDesc(String notes);
}
