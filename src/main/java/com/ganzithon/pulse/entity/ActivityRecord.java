package com.ganzithon.pulse.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_records", indexes = @Index(name = "idx_record_user_created", columnList = "userId, createdAt"))
public class ActivityRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String activityName;

    private String pictureId;
    private String alertId;
    private Integer beforeScore;

    @Column(nullable = false)
    private int afterScore;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ActivityRecord() {}

    public ActivityRecord(Long userId, String activityName, String pictureId, String alertId,
                          Integer beforeScore, int afterScore) {
        this.userId = userId;
        this.activityName = activityName;
        this.pictureId = pictureId;
        this.alertId = alertId;
        this.beforeScore = beforeScore;
        this.afterScore = afterScore;
    }

    public Long getId() { return id; }
    public String getActivityName() { return activityName; }
    public String getPictureId() { return pictureId; }
    public Integer getBeforeScore() { return beforeScore; }
    public int getAfterScore() { return afterScore; }
    public Instant getCreatedAt() { return createdAt; }
}
