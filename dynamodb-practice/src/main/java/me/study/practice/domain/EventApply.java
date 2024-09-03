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
public class EventApply {
    private String partitionKey; //  "user-id:1"
    private String sortKey; // "event-id:;event-term:1"
    @Getter
    @Setter
    private String rewardId;
    @Getter
    @Setter
    private String applyStatus;
    @Getter
    @Setter
    private LocalDateTime createdAt;
    @Getter
    @Setter
    private LocalDateTime updatedAt;

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

    public void setSortKey(final String sortKey) {
        this.sortKey = sortKey;
    }
}
