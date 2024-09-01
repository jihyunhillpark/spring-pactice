package me.study.practice.compare.mariadb.controller;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EventApplyRequest {
    private String eventId;
    private String rewardType;
    private Long userId;
    private String applyStatus;
}
