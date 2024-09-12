package com.indium.assignment.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer matchId;

    private String city;
    private LocalDateTime dates;
    private Integer matchNumber;
    private String eventName;
    private String matchType;
    private String gender;
    private String season;
    private String tossWinner;
    private String tossDecision;
    private String winner;
    private Integer outcomeByWickets;
    private Integer overs;
    private String playerOfMatch;

    // Getters and setters
}