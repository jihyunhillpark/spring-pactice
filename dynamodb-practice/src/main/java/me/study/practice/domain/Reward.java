package me.study.practice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;

@Builder
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class Reward {
    private String partitionKey; //  "event-id:test-20240101;event-term:1;"
    private String sortKey; // "reward-id:COUPON"
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String rewardType;
    @Getter
    @Setter
    private String eventId;
    @Getter
    @Setter
    private int stockCount;

    @Getter
    @Setter
    private LocalDateTime createdAt;
    @Getter
    @Setter
    private LocalDateTime updatedAt;
    @Getter
    @Setter
    private LocalDateTime validFrom;
    @Getter
    @Setter
    private LocalDateTime validTo;


    @DynamoDbPartitionKey
    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    @DynamoDbSortKey
    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }
}
