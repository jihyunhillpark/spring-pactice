package me.study.practice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private String partitionKey; //  "event-id:"
    private String sortKey = "event-term:1";
    @Getter
    @Setter
    private List<String> rewardIds;
    @Getter
    @Setter
    private LocalDateTime startAt;
    @Getter
    @Setter
    private LocalDateTime endAt;
    @Getter
    @Setter
    private LocalDateTime createdAt;
    @Getter
    @Setter
    private LocalDateTime updatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("partitionKey")
    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sortKey")
    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(final String sortKey) {
        this.sortKey = sortKey;
    }
}
