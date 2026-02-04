package com.frostyfox.ethosbackend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "drivers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Driver {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String driverName;
    
    private String route;
    
    private Integer assignedPackages;
    
    private Integer currentlyHeld;
    
    private Integer totalCapacity;
    
    @Column(columnDefinition = "TEXT")
    private String assignedPackageIds; // JSON string of package IDs
    
    private String status; // ACTIVE, INACTIVE, BUSY
    
    private String createdAt;
}
