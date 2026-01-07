package com.tearsdeepmind.repository;

import com.tearsdeepmind.domain.model.CrawlerJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerJobRepository extends JpaRepository<CrawlerJobEntity, String> {
}
