package com.frostyfox.ethosbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "package_priorities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PackagePriority {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String packageId;
    
    private Integer currentPriority;
    
    private String deliveryType;
    
    private String pickupLocation;
    
    private String destination;
    
    private String deliveryTime;
    
    private Double ethicalScore;
    
    @Column(columnDefinition = "TEXT")
    private String pythonResponse;
    
    @Column(columnDefinition = "TEXT")
    private String explanation;
    
    private String assignedDriver;
    
    private String createdAt;
}
