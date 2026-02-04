package com.frostyfox.ethosbackend.repository;

import com.frostyfox.ethosbackend.model.PackagePriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PackagePriorityRepository extends JpaRepository<PackagePriority, Long> {
    
    @Query("SELECT p FROM PackagePriority p WHERE p.currentPriority <= ?1 ORDER BY p.currentPriority ASC")
    List<PackagePriority> findPackagesWithPriorityOrHigher(Integer priority);
    
    @Query("SELECT p FROM PackagePriority p WHERE p.currentPriority >= ?1 ORDER BY p.currentPriority ASC")
    List<PackagePriority> findPackagesWithPriorityOrLower(Integer priority);
    
    @Query("SELECT p FROM PackagePriority p ORDER BY p.ethicalScore DESC")
    List<PackagePriority> findAllOrderByEthicalScoreDesc();
}
