package com.tearsdeepmind.repository.market;

import com.tearsdeepmind.entity.market.TechnicalIndicatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TechnicalIndicatorRepository extends JpaRepository<TechnicalIndicatorEntity, Long> {
    Optional<TechnicalIndicatorEntity> findBySymbolAndDate(String symbol, LocalDate date);
}
