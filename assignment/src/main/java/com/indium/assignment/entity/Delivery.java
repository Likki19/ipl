package com.indium.assignment.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "deliveries")
@Data
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer deliveryId;

    private Integer overNumber;
    private Integer ballNumber;
    private String batter;
    private String bowler;
    private String nonStriker;
    private Integer runsBatter;
    private Integer runsExtras;
    private Integer runsTotal;
    private String wicketType;
    private String playerOut;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Match match;

    // Getters and setters
}
