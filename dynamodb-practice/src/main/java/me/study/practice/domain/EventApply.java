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
public class EventApply {
    private final String dataType = AttributesDataType.EVENT_APPLY.name();
    private String eventId;
    private String userId;
    private String prizeType;

    @DynamoDbPartitionKey
    public String getDataType() {
        return dataType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPrizeType() {
        return prizeType;
    }

    public void setPrizeType(String prizeType) {
        this.prizeType = prizeType;
    }

    @DynamoDbSortKey
    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }
}
