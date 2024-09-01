package me.study.practice.compare.mariadb.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@ToString
public class EventApply {

    @Id
    private String id;

    private String eventId;
    private Long userId;
    private String rewardId;
    private String applyStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getEventId() {
        return this.id;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(final long userId) {
        this.userId = userId;
    }

    public void setRewardId(final String rewardId) {
        this.rewardId = rewardId;
    }

    public void setApplyStatus(final String applyStatus) {
        this.applyStatus = applyStatus;
    }

    public String getApplyStatus() {
        return applyStatus;
    }

    public void setId(final String id) {
        this.id = id;
    }
}
