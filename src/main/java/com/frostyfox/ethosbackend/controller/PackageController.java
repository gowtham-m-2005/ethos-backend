package com.frostyfox.ethosbackend.controller;

import com.frostyfox.ethosbackend.model.PackagePriority;
import com.frostyfox.ethosbackend.service.EthosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PackageController {
    
    private final EthosService ethosService;
    
    @GetMapping
    public ResponseEntity<List<PackagePriority>> getAllPackages() {
        List<PackagePriority> packages = ethosService.getAllPackages();
        return ResponseEntity.ok(packages);
    }
    
    @GetMapping("/delivery-type/{type}")
    public ResponseEntity<List<PackagePriority>> getPackagesByDeliveryType(@PathVariable String type) {
        List<PackagePriority> packages = ethosService.getPackagesByDeliveryType(type);
        return ResponseEntity.ok(packages);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPackageStats() {
        Map<String, Object> stats = ethosService.getPackageStats();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PackagePriority> getPackageById(@PathVariable Long id) {
        PackagePriority packagePriority = ethosService.getPackageById(id);
        return packagePriority != null ? 
            ResponseEntity.ok(packagePriority) : 
            ResponseEntity.notFound().build();
    }
    
    @GetMapping("/{id}/explanation")
    public ResponseEntity<Map<String, Object>> getPackageExplanation(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        Map<String, Object> explanation = ethosService.getPackageExplanation(id, force);
        return explanation != null ? 
            ResponseEntity.ok(explanation) : 
            ResponseEntity.notFound().build();
    }
    
    @PostMapping("/populate-explanations")
    public ResponseEntity<Map<String, String>> populateExistingPackageExplanations() {
        String result = ethosService.populateExistingPackageExplanations();
        return ResponseEntity.ok(Map.of("message", result));
    }
}
