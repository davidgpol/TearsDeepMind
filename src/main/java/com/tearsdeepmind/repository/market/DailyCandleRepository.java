package com.tearsdeepmind.repository.market;

import com.tearsdeepmind.entity.market.DailyCandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyCandleRepository extends JpaRepository<DailyCandleEntity, Long> {
    Optional<DailyCandleEntity> findBySymbolAndDate(String symbol, LocalDate date);
    List<DailyCandleEntity> findBySymbolOrderByDateDesc(String symbol);
}
