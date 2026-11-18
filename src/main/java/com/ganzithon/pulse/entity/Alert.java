package com.ganzithon.pulse.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {
    public enum Status { PENDING, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 앱에 내려주는 ID (예: alert_20261115_0001) */
    @Column(unique = true)
    private String alertId;

    @Column(nullable = false)
    private Long userId;

    private int bpm;
    private int baselineBpm;
    private Integer steps;

    @Column(nullable = false)
    private Instant measuredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Alert() {}

    public Alert(Long userId, int bpm, int baselineBpm, Integer steps, Instant measuredAt) {
        this.userId = userId;
        this.bpm = bpm;
        this.baselineBpm = baselineBpm;
        this.steps = steps;
        this.measuredAt = measuredAt;
    }

    public void assignAlertId(String alertId) { this.alertId = alertId; }
    public void dismiss() { this.status = Status.DISMISSED; }

    public Long getId() { return id; }
    public String getAlertId() { return alertId; }
    public Long getUserId() { return userId; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
