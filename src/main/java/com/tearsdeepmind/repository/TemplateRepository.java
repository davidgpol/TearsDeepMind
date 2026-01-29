package com.tearsdeepmind.repository;

import com.tearsdeepmind.domain.model.TemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TemplateRepository extends JpaRepository<TemplateEntity, String> {
}
