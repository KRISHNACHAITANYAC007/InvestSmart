package com.example.investsmart.investment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IpoApplicationRepository extends JpaRepository<IpoApplication,Long> {
    Optional<IpoApplication> findByUserIdAndIpoId(Long userId,String ipoId);
    List<IpoApplication> findByUserIdOrderByIdDesc(Long userId);
    List<IpoApplication> findByStatusIn(Collection<String> statuses);
}
