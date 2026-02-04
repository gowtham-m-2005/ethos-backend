package com.frostyfox.ethosbackend.controller;

import com.frostyfox.ethosbackend.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/distribution")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DistributionController {
    
    private final DistributionService distributionService;
    
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getDistribution() {
        List<Map<String, Object>> distribution = distributionService.getDriverDistribution();
        return ResponseEntity.ok(distribution);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Integer>> getDistributionStats() {
        Map<String, Integer> stats = distributionService.getDistributionStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/packages")
    public ResponseEntity<List<Map<String, Object>>> getPackagesByStatus(@RequestParam String status) {
        List<Map<String, Object>> packages = distributionService.getPackagesByStatus(status);
        return ResponseEntity.ok(packages);
    }
    
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, String>> initializeDistribution() {
        String result = distributionService.initializeDriversAndAssignPackages();
        return ResponseEntity.ok(Map.of("message", result));
    }
    
    @PostMapping("/force-reinitialize")
    public ResponseEntity<Map<String, Object>> forceReinitializeDistribution() {
        Map<String, Object> result = distributionService.forceReinitializeDriversAndAssignPackages();
        return ResponseEntity.ok(result);
    }
}
