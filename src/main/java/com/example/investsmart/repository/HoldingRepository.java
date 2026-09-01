package com.example.investsmart.repository;

import com.example.investsmart.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
public interface HoldingRepository extends JpaRepository<Holding, Long>  {

    Optional<Holding> findByUserIdAndStockId(Long userId, Long stockId);
    List<Holding> findByUserId(Long userId);
}