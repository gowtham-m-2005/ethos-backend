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
    private final DistributionService distributionService;
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
            packagePriority.setExplanation(generateSimpleExplanation(packagePriority));
            packagePriority.setCreatedAt(timestamp);
            
            packagePriorityRepository.save(packagePriority);
            
            // Recalculate all priorities based on ethical scores
            recalculateAllPriorities();
            
            // Assign new package to driver automatically
            distributionService.assignNewPackageToDriver(packagePriority);
            
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
    
    public String populateExistingPackageExplanations() {
        try {
            List<PackagePriority> allPackages = packagePriorityRepository.findAll();
            int updatedCount = 0;
            
            for (PackagePriority pkg : allPackages) {
                if (pkg.getExplanation() == null || pkg.getExplanation().isEmpty()) {
                    pkg.setExplanation(generateSimpleExplanation(pkg));
                    packagePriorityRepository.save(pkg);
                    updatedCount++;
                }
            }
            
            log.info("Populated explanations for {} existing packages", updatedCount);
            return "Successfully populated explanations for " + updatedCount + " packages";
            
        } catch (Exception e) {
            log.error("Error populating existing package explanations", e);
            return "Error: " + e.getMessage();
        }
    }
    
    public Map<String, Object> getPackageExplanation(Long id, boolean forceRegenerate) {
        PackagePriority pkg = packagePriorityRepository.findById(id).orElse(null);
        
        if (pkg == null) {
            return null;
        }
        
        // If no explanation exists or force=true, generate and store it
        if (pkg.getExplanation() == null || forceRegenerate) {
            String explanation = generateSimpleExplanation(pkg);
            pkg.setExplanation(explanation);
            packagePriorityRepository.save(pkg);
            return Map.of(
                "packageId", pkg.getPackageId(),
                "explanation", explanation,
                "source", "generated"
            );
        }
        
        // Return existing explanation
        return Map.of(
            "packageId", pkg.getPackageId(),
            "explanation", pkg.getExplanation(),
            "source", "persisted"
        );
    }
    
    private String generateSimpleExplanation(PackagePriority pkg) {
        StringBuilder explanation = new StringBuilder();
        
        // Start with delivery type context
        if ("MEDICAL_EXPRESS".equals(pkg.getDeliveryType())) {
            explanation.append("This medical package from ").append(pkg.getPickupLocation())
                      .append(" to ").append(pkg.getDestination())
                      .append(" contains critical medical supplies with an ethical score of ")
                      .append(pkg.getEthicalScore()).append(", ");
            
            if (pkg.getEthicalScore() >= 8.0) {
                explanation.append("requiring immediate delivery as delays could cause serious harm to patients.");
            } else if (pkg.getEthicalScore() >= 6.0) {
                explanation.append("needing priority delivery to ensure timely medical treatment.");
            } else {
                explanation.append("requiring careful handling and timely delivery for patient care.");
            }
            
        } else if ("FOOD_EXPRESS".equals(pkg.getDeliveryType())) {
            explanation.append("This food package traveling from ").append(pkg.getPickupLocation())
                      .append(" to ").append(pkg.getDestination())
                      .append(" has an ethical score of ").append(pkg.getEthicalScore())
                      .append(" and ");
            
            if (pkg.getEthicalScore() >= 6.0) {
                explanation.append("contains perishable items that will spoil if not delivered quickly by ")
                          .append(pkg.getDeliveryTime()).append(".");
            } else {
                explanation.append("requires timely delivery to maintain food quality for the recipient.");
            }
            
        } else if ("ESSENTIAL".equals(pkg.getDeliveryType())) {
            explanation.append("This essential package from ").append(pkg.getPickupLocation())
                      .append(" to ").append(pkg.getDestination())
                      .append(" has an ethical score of ").append(pkg.getEthicalScore())
                      .append(" and ");
            
            if (pkg.getEthicalScore() >= 7.0) {
                explanation.append("contains items that someone urgently needs, requiring priority delivery by ")
                          .append(pkg.getDeliveryTime()).append(".");
            } else {
                explanation.append("contains important items needed by the recipient, warranting priority handling.");
            }
            
        } else {
            // Standard packages with contextual details
            explanation.append("This package from ").append(pkg.getPickupLocation())
                      .append(" to ").append(pkg.getDestination())
                      .append(" has an ethical score of ").append(pkg.getEthicalScore());
            
            if (pkg.getEthicalScore() >= 5.0) {
                explanation.append(" and requires priority delivery due to its importance level.");
            } else if (pkg.getEthicalScore() >= 3.0) {
                explanation.append(" and should be delivered by ").append(pkg.getDeliveryTime())
                          .append(" to meet the recipient's needs.");
            } else {
                explanation.append(" and can be delivered with standard priority handling.");
            }
        }
        
        // Add priority context
        explanation.append(" It is assigned Priority ").append(pkg.getCurrentPriority())
                  .append(" based on its relative importance compared to other packages.");
        
        return explanation.toString();
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
