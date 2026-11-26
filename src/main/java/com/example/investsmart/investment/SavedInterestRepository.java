package com.example.investsmart.investment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SavedInterestRepository extends JpaRepository<SavedInterest,Long> {
    List<SavedInterest> findByUserIdOrderByIdDesc(Long userId);
    Optional<SavedInterest> findByUserIdAndKindAndReferenceId(Long userId,String kind,String referenceId);
}
