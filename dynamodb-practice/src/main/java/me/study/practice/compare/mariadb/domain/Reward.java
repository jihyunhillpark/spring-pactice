package me.study.practice.compare.mariadb.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
public class Reward {

    @Id
    private String id;

    private String name;
    private String rewardType;
    private String eventId;
    private int stockCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;

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

}
