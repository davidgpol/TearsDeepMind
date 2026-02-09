package com.tearsdeepmind.repository.market;

import com.tearsdeepmind.entity.market.IntradayCandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IntradayCandleRepository extends JpaRepository<IntradayCandleEntity, Long> {
    Optional<IntradayCandleEntity> findBySymbolAndTimestamp(String symbol, LocalDateTime timestamp);
    List<IntradayCandleEntity> findBySymbolAndTimestampBetweenOrderByTimestampAsc(String symbol, LocalDateTime start, LocalDateTime end);
}
