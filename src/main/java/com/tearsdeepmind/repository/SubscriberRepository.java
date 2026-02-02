package com.tearsdeepmind.repository;

import com.tearsdeepmind.entity.SubscriberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriberRepository extends JpaRepository<SubscriberEntity, String> {
    List<SubscriberEntity> findByIsActiveTrue();
}
