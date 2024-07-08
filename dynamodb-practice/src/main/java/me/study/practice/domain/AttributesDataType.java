package me.study.practice.domain;

import java.util.Arrays;

public enum AttributesDataType {
    EVENT_APPLY("EventApply"),
    PRIZE("Prize");

    AttributesDataType(final String type) {}

    AttributesDataType AttributesDataType(final String type) {
        return Arrays.stream(AttributesDataType.values())
                     .filter(attributesDataType -> attributesDataType.name().equalsIgnoreCase(type))
                     .findAny()
                     .orElseThrow(() -> new IllegalArgumentException("Invalid attribute type: " + type));
    }
}
