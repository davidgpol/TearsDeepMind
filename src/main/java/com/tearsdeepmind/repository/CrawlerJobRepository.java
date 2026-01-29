package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.CrawlerJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CrawlerJobRepository extends JpaRepository<CrawlerJobEntity, String> {
}
