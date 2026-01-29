package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.QuantMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuantMemoryRepository extends JpaRepository<QuantMemoryEntity, String> {
}
