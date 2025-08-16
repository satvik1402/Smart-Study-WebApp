package com.smartstudy.repository;

import com.smartstudy.model.AnalyticsCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalyticsCounterRepository extends JpaRepository<AnalyticsCounter, Long> {
    Optional<AnalyticsCounter> findTopByOrderByIdAsc();
}
