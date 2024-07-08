package me.study.practice.domain;

import java.util.Arrays;

public enum PrizeType {
    POINT("point"),
    COUPON("coupon");

    PrizeType(final String prizeType) {}

    PrizeType PrizeType(final String prizeType) {
        return Arrays.stream(PrizeType.values())
                     .filter(prize -> prize.name().equalsIgnoreCase(prizeType))
                     .findAny()
                     .orElseThrow(() -> new IllegalArgumentException("Invalid prize type: " + prizeType));
    }
}
