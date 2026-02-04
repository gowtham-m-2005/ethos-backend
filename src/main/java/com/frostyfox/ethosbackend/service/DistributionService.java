package com.frostyfox.ethosbackend.service;

import com.frostyfox.ethosbackend.model.Driver;
import com.frostyfox.ethosbackend.model.PackagePriority;
import com.frostyfox.ethosbackend.repository.DriverRepository;
import com.frostyfox.ethosbackend.repository.PackagePriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistributionService {
    
    private final DriverRepository driverRepository;
    private final PackagePriorityRepository packagePriorityRepository;
    
    private boolean initialized = false;
    
    public List<Map<String, Object>> getDriverDistribution() {
        // Check if packages exist in database
        long packageCount = packagePriorityRepository.count();
        log.info("Database contains {} packages", packageCount);
        
        // Auto-initialize if no drivers exist
        if (!initialized && driverRepository.count() == 0) {
            log.info("Auto-initializing drivers and assigning packages...");
            if (packageCount == 0) {
                log.warn("No packages found in database. Cannot assign packages to drivers.");
                // Create empty drivers anyway
                List<Driver> drivers = createDrivers();
                driverRepository.saveAll(drivers);
                initialized = true;
            } else {
                initializeDriversAndAssignPackages();
                initialized = true;
            }
        }
        
        // Check for unassigned packages and assign them
        assignUnassignedPackages();
        
        List<Driver> drivers = driverRepository.findAll();
        
        return drivers.stream()
            .map(driver -> {
                Map<String, Object> driverInfo = new HashMap<>();
                driverInfo.put("driverName", driver.getDriverName());
                driverInfo.put("assignedPackages", driver.getAssignedPackages());
                driverInfo.put("currentlyHeld", driver.getCurrentlyHeld());
                driverInfo.put("totalCapacity", driver.getTotalCapacity());
                return driverInfo;
            })
            .collect(Collectors.toList());
    }
    
    public Map<String, Integer> getDistributionStats() {
        List<PackagePriority> allPackages = packagePriorityRepository.findAll();
        
        int totalReceived = allPackages.size();
        int assigned = (int) allPackages.stream().filter(pkg -> 
            pkg.getPythonResponse() != null && !pkg.getPythonResponse().isEmpty()
        ).count();
        
        int ready = (int) allPackages.stream().filter(pkg -> 
            pkg.getCurrentPriority() <= 3
        ).count();
        
        int critical = (int) allPackages.stream().filter(pkg -> 
            pkg.getCurrentPriority() == 1
        ).count();
        
        return Map.of(
            "totalReceived", totalReceived,
            "assigned", assigned,
            "ready", ready,
            "critical", critical
        );
    }
    
    public List<Map<String, Object>> getPackagesByStatus(String status) {
        List<PackagePriority> allPackages = packagePriorityRepository.findAll();
        List<PackagePriority> filteredPackages = new ArrayList<>();
        
        switch (status.toLowerCase()) {
            case "assigned":
                filteredPackages = allPackages.stream()
                    .filter(pkg -> pkg.getPythonResponse() != null && !pkg.getPythonResponse().isEmpty())
                    .collect(Collectors.toList());
                break;
            case "ready":
                filteredPackages = allPackages.stream()
                    .filter(pkg -> pkg.getCurrentPriority() <= 3)
                    .collect(Collectors.toList());
                break;
            case "critical":
                filteredPackages = allPackages.stream()
                    .filter(pkg -> pkg.getCurrentPriority() == 1)
                    .collect(Collectors.toList());
                break;
            case "received":
                filteredPackages = allPackages; // All packages
                break;
            default:
                filteredPackages = new ArrayList<>();
        }
        
        return filteredPackages.stream()
            .map(pkg -> {
                Map<String, Object> packageInfo = new HashMap<>();
                packageInfo.put("id", pkg.getId());
                packageInfo.put("deliveryType", pkg.getDeliveryType());
                packageInfo.put("pickupLocation", pkg.getPickupLocation());
                packageInfo.put("destination", pkg.getDestination());
                return packageInfo;
            })
            .collect(Collectors.toList());
    }
    
    public String initializeDriversAndAssignPackages() {
        try {
            // Check if drivers already exist
            if (driverRepository.count() > 0) {
                return "Drivers already initialized. Use force=true to reinitialize.";
            }
            
            // Create 3 drivers
            List<Driver> drivers = createDrivers();
            
            // Get all packages and assign to drivers
            List<PackagePriority> allPackages = packagePriorityRepository.findAllOrderByEthicalScoreDesc();
            log.info("Found {} packages to assign to drivers", allPackages.size());
            assignPackagesToDrivers(drivers, allPackages);
            
            // Save drivers
            driverRepository.saveAll(drivers);
            
            log.info("Initialized {} drivers and assigned {} packages", drivers.size(), allPackages.size());
            return "Successfully initialized 3 drivers and assigned packages";
            
        } catch (Exception e) {
            log.error("Error initializing drivers", e);
            return "Error initializing drivers: " + e.getMessage();
        }
    }
    
    public Map<String, Object> forceReinitializeDriversAndAssignPackages() {
        try {
            // Delete all existing drivers
            driverRepository.deleteAll();
            log.info("Deleted all existing drivers");
            
            // Reset initialization flag
            initialized = false;
            
            // Get all packages
            List<PackagePriority> allPackages = packagePriorityRepository.findAllOrderByEthicalScoreDesc();
            log.info("Found {} packages in database", allPackages.size());
            
            // Create and assign drivers
            List<Driver> drivers = createDrivers();
            assignPackagesToDrivers(drivers, allPackages);
            driverRepository.saveAll(drivers);
            
            // Return detailed results
            List<Map<String, Object>> driverResults = drivers.stream()
                .map(driver -> {
                    Map<String, Object> driverMap = new HashMap<>();
                    driverMap.put("driverName", driver.getDriverName());
                    driverMap.put("assignedPackages", driver.getAssignedPackages());
                    driverMap.put("currentlyHeld", driver.getCurrentlyHeld());
                    driverMap.put("totalCapacity", driver.getTotalCapacity());
                    return driverMap;
                })
                .collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "Force reinitialized drivers and assigned packages");
            result.put("totalPackages", allPackages.size());
            result.put("drivers", driverResults);
            
            return result;
            
        } catch (Exception e) {
            log.error("Error force reinitializing drivers", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("message", "Error: " + e.getMessage());
            errorResult.put("error", true);
            return errorResult;
        }
    }
    
    private void assignUnassignedPackages() {
        try {
            // Find all packages with no assigned driver OR with JSON array format
            List<PackagePriority> unassignedPackages = packagePriorityRepository.findAll().stream()
                .filter(pkg -> pkg.getAssignedDriver() == null || 
                           pkg.getAssignedDriver().isEmpty() || 
                           pkg.getAssignedDriver().startsWith("[") ||
                           pkg.getAssignedDriver().contains("PKG-"))
                .collect(Collectors.toList());
            
            if (!unassignedPackages.isEmpty()) {
                log.info("Found {} unassigned packages, assigning them now...", unassignedPackages.size());
                
                // Ensure drivers exist
                if (driverRepository.count() == 0) {
                    List<Driver> drivers = createDrivers();
                    driverRepository.saveAll(drivers);
                }
                
                // Clear existing incorrect assignments and reassign
                for (PackagePriority pkg : unassignedPackages) {
                    // Clear incorrect assignedDriver value
                    pkg.setAssignedDriver(null);
                    packagePriorityRepository.save(pkg);
                    
                    // Assign to driver properly
                    assignNewPackageToDriver(pkg);
                }
                
                log.info("Successfully assigned {} unassigned packages to drivers", unassignedPackages.size());
            }
        } catch (Exception e) {
            log.error("Error assigning unassigned packages", e);
        }
    }
    
    public void assignNewPackageToDriver(PackagePriority newPackage) {
        try {
            // Ensure drivers exist
            if (driverRepository.count() == 0) {
                log.info("No drivers exist, creating drivers first...");
                List<Driver> drivers = createDrivers();
                driverRepository.saveAll(drivers);
            }
            
            // Get driver with least packages (round-robin based on current count)
            Driver driver = driverRepository.findAllByOrderByAssignedPackagesAsc().stream()
                .findFirst()
                .orElse(driverRepository.findAll().get(0));
            
            // Update package with driver assignment
            newPackage.setAssignedDriver(driver.getDriverName());
            packagePriorityRepository.save(newPackage);
            
            // Update driver assignment
            driver.setAssignedPackages(driver.getAssignedPackages() + 1);
            driver.setCurrentlyHeld(driver.getCurrentlyHeld() + 1);
            
            // Add package to assigned list
            List<String> assignedPkgIds = parseAssignedPackageIds(driver.getAssignedPackageIds());
            assignedPkgIds.add(newPackage.getPackageId());
            driver.setAssignedPackageIds(assignedPkgIds.toString());
            
            driverRepository.save(driver);
            
            log.info("Assigned package {} to driver {} (total: {})", 
                newPackage.getPackageId(), driver.getDriverName(), driver.getAssignedPackages());
                
        } catch (Exception e) {
            log.error("Error assigning new package to driver", e);
        }
    }
    
    private List<Driver> createDrivers() {
        List<Driver> drivers = new ArrayList<>();
        
        // Driver 1 - Handles high priority packages
        Driver driver1 = new Driver();
        driver1.setDriverName("Driver 1");
        driver1.setRoute("North Route - Medical & Emergency");
        driver1.setAssignedPackages(0);
        driver1.setCurrentlyHeld(0);
        driver1.setTotalCapacity(5);
        driver1.setAssignedPackageIds("[]");
        driver1.setStatus("ACTIVE");
        driver1.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        drivers.add(driver1);
        
        // Driver 2 - Handles medium priority packages
        Driver driver2 = new Driver();
        driver2.setDriverName("Driver 2");
        driver2.setRoute("Central Route - Food & Essential");
        driver2.setAssignedPackages(0);
        driver2.setCurrentlyHeld(0);
        driver2.setTotalCapacity(5);
        driver2.setAssignedPackageIds("[]");
        driver2.setStatus("ACTIVE");
        driver2.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        drivers.add(driver2);
        
        // Driver 3 - Handles standard packages
        Driver driver3 = new Driver();
        driver3.setDriverName("Driver 3");
        driver3.setRoute("South Route - Standard & General");
        driver3.setAssignedPackages(0);
        driver3.setCurrentlyHeld(0);
        driver3.setTotalCapacity(5);
        driver3.setAssignedPackageIds("[]");
        driver3.setStatus("ACTIVE");
        driver3.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        drivers.add(driver3);
        
        return drivers;
    }
    
    private void assignPackagesToDrivers(List<Driver> drivers, List<PackagePriority> packages) {
        // Initialize package counts for all drivers
        drivers.forEach(driver -> {
            driver.setAssignedPackages(0);
            driver.setCurrentlyHeld(0);
        });
        
        // Round-robin assignment based on priority
        for (int i = 0; i < packages.size(); i++) {
            PackagePriority pkg = packages.get(i);
            Driver driver = drivers.get(i % 3); // Rotate among 3 drivers
            
            // Update package with driver assignment
            pkg.setAssignedDriver(driver.getDriverName());
            
            // Update assigned packages count
            driver.setAssignedPackages(driver.getAssignedPackages() + 1);
            
            // Update currently held (for now, same as assigned - can be updated later for real-time tracking)
            driver.setCurrentlyHeld(driver.getAssignedPackages());
            
            // Add package to assigned packages list
            try {
                List<String> assignedPkgIds = parseAssignedPackageIds(driver.getAssignedPackageIds());
                assignedPkgIds.add(pkg.getPackageId());
                driver.setAssignedPackageIds(assignedPkgIds.toString());
            } catch (Exception e) {
                log.error("Error updating assigned packages for driver {}", driver.getDriverName(), e);
            }
        }
        
        // Save all updated packages with driver assignments
        packagePriorityRepository.saveAll(packages);
        
        // Log final assignment
        drivers.forEach(driver -> {
            log.info("Driver {}: {} assigned, {} currently held, capacity {}", 
                driver.getDriverName(), driver.getAssignedPackages(), 
                driver.getCurrentlyHeld(), driver.getTotalCapacity());
        });
    }
    
    private List<String> parseAssignedPackageIds(String assignedPackageIds) {
        try {
            // Parse JSON array string to list
            if (assignedPackageIds == null || assignedPackageIds.equals("[]")) {
                return new ArrayList<>();
            }
            return Arrays.asList(assignedPackageIds.replaceAll("[\\[\\]]", "").split(",\\s*"))
                         .stream()
                         .filter(s -> !s.isEmpty())
                         .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
