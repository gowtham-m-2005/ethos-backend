package com.frostyfox.ethosbackend.service;

import com.frostyfox.ethosbackend.model.EthosModel;
import com.frostyfox.ethosbackend.model.PackagePriority;
import com.frostyfox.ethosbackend.repository.EthosRepository;
import com.frostyfox.ethosbackend.repository.PackagePriorityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.LinkedHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class EthosService {
    private final EthosRepository ethosRepository;
    private final PackagePriorityRepository packagePriorityRepository;
    private final WebClient webClient;

    public String getEthos(EthosModel ethosModel){
        return ethosRepository.save(ethosModel).getPackageDescription();
    }

    public Object forwardToPython(EthosModel ethosModel) {

        Map<String, String> payload = Map.of(
                "description", ethosModel.getPackageDescription()
        );

        Object response = webClient.post()
                .uri("/ai/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .block();

        System.out.println("TTTTTTTTTTTTTTHHHHHHHHEEEEEEEEEEE");
        // Save package priority to database
        savePackagePriority(ethosModel, response);
        
        log.info("Python API Response: {}", response);
        System.out.println("=== /api/ethos Response ===");
        System.out.println("Response: " + response);
        System.out.println("==========================");
        
        return response;
    }
    
    private void savePackagePriority(EthosModel ethosModel, Object pythonResponse) {
        try {
            String responseJson = pythonResponse.toString();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Parse the response to extract ethical score and priority
            Double ethicalScore = extractEthicalScore(pythonResponse);
            Integer priority = calculatePriority(ethicalScore);
            
            PackagePriority packagePriority = new PackagePriority();
            packagePriority.setPackageId("PKG-" + System.currentTimeMillis()); // Generate unique package ID
            packagePriority.setCurrentPriority(priority);
            packagePriority.setDeliveryType(determineDeliveryType(pythonResponse));
            packagePriority.setPickupLocation(ethosModel.getPickupLocation());
            packagePriority.setDestination(ethosModel.getDestination());
            packagePriority.setDeliveryTime(ethosModel.getDeliveryTime());
            packagePriority.setEthicalScore(ethicalScore);
            packagePriority.setPythonResponse(responseJson);
            packagePriority.setCreatedAt(timestamp);
            
            packagePriorityRepository.save(packagePriority);
            
            // Recalculate all priorities based on ethical scores
            recalculateAllPriorities();
            
            log.info("Package priority saved with ID: {}, Priority: {}, Ethical Score: {}", 
                    packagePriority.getId(), priority, ethicalScore);
            
        } catch (Exception e) {
            log.error("Failed to save package priority", e);
        }
    }
    
    private Double extractEthicalScore(Object response) {
        try {
            if (response instanceof LinkedHashMap) {
                LinkedHashMap<?, ?> responseMap = (LinkedHashMap<?, ?>) response;
                Object scoreObj = responseMap.get("score");
                if (scoreObj instanceof LinkedHashMap) {
                    LinkedHashMap<?, ?> scoreMap = (LinkedHashMap<?, ?>) scoreObj;
                    Object totalScore = scoreMap.get("total_score");
                    if (totalScore != null) {
                        return Double.parseDouble(totalScore.toString());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extracting ethical score", e);
        }
        return 0.0; // Default score if extraction fails
    }
    
    private Integer calculatePriority(Double ethicalScore) {
        if (ethicalScore >= 8.0) {
            return 1; // Critical Priority
        } else if (ethicalScore >= 6.0) {
            return 2; // High Priority
        } else if (ethicalScore >= 4.0) {
            return 3; // Medium Priority
        } else {
            return 4; // Low Priority
        }
    }
    
    private String determineDeliveryType(Object pythonResponse) {
        try {
            if (pythonResponse instanceof LinkedHashMap) {
                LinkedHashMap<?, ?> responseMap = (LinkedHashMap<?, ?>) pythonResponse;
                String explanation = responseMap.get("explanation") != null ? 
                    responseMap.get("explanation").toString() : "";
                
                // Extract domain from explanation
                String domain = extractDomainFromExplanation(explanation);
                System.out.println("Extracted domain: " + domain);
                
                return domain.toUpperCase();
            }
        } catch (Exception e) {
            log.error("Error determining delivery type", e);
        }
        return "STANDARD";
    }
    
    private String extractDomainFromExplanation(String explanation) {
        try {
            // Look for pattern "associated with the DOMAIN domain."
            String pattern = "associated with the ";
            int startIndex = explanation.indexOf(pattern);
            if (startIndex != -1) {
                startIndex += pattern.length();
                int endIndex = explanation.indexOf(" domain.", startIndex);
                if (endIndex != -1) {
                    String domain = explanation.substring(startIndex, endIndex);
                    System.out.println("Found domain: " + domain);
                    return domain.toUpperCase();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract domain from explanation", e);
        }
        return "STANDARD";
    }
    
    private void recalculateAllPriorities() {
        try {
            // Get all packages ordered by ethical score (highest first)
            List<PackagePriority> allPackages = packagePriorityRepository.findAllOrderByEthicalScoreDesc();
            
            int updatedCount = 0;
            for (int i = 0; i < allPackages.size(); i++) {
                PackagePriority pkg = allPackages.get(i);
                Integer newPriority = i + 1; // Priority 1 for highest score, 2 for second highest, etc.
                
                // Only update if priority actually changed
                if (!pkg.getCurrentPriority().equals(newPriority)) {
                    Integer oldPriority = pkg.getCurrentPriority();
                    pkg.setCurrentPriority(newPriority);
                    packagePriorityRepository.save(pkg);
                    updatedCount++;
                    
                    log.info("Updated package ID {} from priority {} to {} (ethical score: {})", 
                        pkg.getId(), oldPriority, newPriority, pkg.getEthicalScore());
                }
            }
            
            if (updatedCount > 0) {
                log.info("Priority recalculation completed: {} packages updated", updatedCount);
                System.out.println("=== PRIORITY RECALCULATION ===");
                System.out.println("Recalculated priorities based on ethical scores");
                System.out.println("Updated " + updatedCount + " packages");
                System.out.println("Highest ethical score now has Priority 1");
                System.out.println("===============================");
            }
            
        } catch (Exception e) {
            log.error("Error during priority recalculation", e);
        }
    }
    
    // Package management methods
    public List<PackagePriority> getAllPackages() {
        return packagePriorityRepository.findAllOrderByEthicalScoreDesc();
    }
    
    public List<PackagePriority> getPackagesByDeliveryType(String deliveryType) {
        return packagePriorityRepository.findAll()
            .stream()
            .filter(pkg -> pkg.getDeliveryType().equalsIgnoreCase(deliveryType))
            .toList();
    }
    
    public Map<String, Object> getPackageStats() {
        List<PackagePriority> allPackages = packagePriorityRepository.findAll();
        
        // Count by priority
        Map<Integer, Long> priorityCounts = allPackages.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                PackagePriority::getCurrentPriority, 
                java.util.stream.Collectors.counting()
            ));
        
        // Count by delivery type
        Map<String, Long> deliveryTypeCounts = allPackages.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                PackagePriority::getDeliveryType, 
                java.util.stream.Collectors.counting()
            ));
        
        // Average ethical score
        double avgEthicalScore = allPackages.stream()
            .mapToDouble(PackagePriority::getEthicalScore)
            .average()
            .orElse(0.0);
        
        return Map.of(
            "totalPackages", allPackages.size(),
            "priorityDistribution", priorityCounts,
            "deliveryTypeDistribution", deliveryTypeCounts,
            "averageEthicalScore", avgEthicalScore,
            "highestPriorityPackage", allPackages.stream()
                .min((a, b) -> b.getEthicalScore().compareTo(a.getEthicalScore()))
                .orElse(null),
            "lowestPriorityPackage", allPackages.stream()
                .min((a, b) -> a.getEthicalScore().compareTo(b.getEthicalScore()))
                .orElse(null)
        );
    }
    
    public PackagePriority getPackageById(Long id) {
        return packagePriorityRepository.findById(id).orElse(null);
    }

//    public void sendEthos(EthosModel ethosModel){
//        webClient.post()
//                .uri("/ai/analyze")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(ethosModel)
//                .retrieve()
//                .toBodilessEntity() // ignore response
//                .block();
//    }
}
