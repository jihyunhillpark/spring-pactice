package me.study.practice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Builder
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class Prize {
    private final String dataType = AttributesDataType.PRIZE.name();
    private String eventId;
    private String rewardType;
    private int stock;
    private int supplied;

    @DynamoDbPartitionKey
    public String getDataType() {
        return dataType;
    }

    @DynamoDbSortKey
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRewardType() {
        return rewardType;
    }

    public void setRewardType(String rewardType) {
        this.rewardType = rewardType;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(final int stock) {
        this.stock = stock;
    }

    public int getSupplied() {
        return supplied;
    }

    public void setSupplied(final int supplied) {
        this.supplied = supplied;
    }

    public Prize decreaseStock() {
        this.stock--;
        this.supplied++;
        return this;
    }
}
