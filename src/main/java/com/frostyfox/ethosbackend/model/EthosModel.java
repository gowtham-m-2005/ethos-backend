package com.frostyfox.ethosbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;


@Entity
@Data
public class EthosModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String senderName;
    private String pickupLocation;
    private String packageDescription;
    private String packageWeight;
    private String deliveryTime;
    private String receiverName;
    private String destination;
}
