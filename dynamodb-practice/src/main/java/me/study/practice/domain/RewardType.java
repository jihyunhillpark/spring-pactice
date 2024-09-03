package me.study.practice.domain;

import java.util.Arrays;

public enum RewardType {
    POINT("point"),
    COUPON("coupon");

    RewardType(final String prizeType) {}

    RewardType PrizeType(final String prizeType) {
        return Arrays.stream(RewardType.values())
                     .filter(prize -> prize.name().equalsIgnoreCase(prizeType))
                     .findAny()
                     .orElseThrow(() -> new IllegalArgumentException("Invalid prize type: " + prizeType));
    }
}
