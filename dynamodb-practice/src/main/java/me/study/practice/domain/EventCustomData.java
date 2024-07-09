package me.study.practice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Map;

@Builder
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
public class EventCustomData {
    private String primaryKey;
    private Map<String, String> contents; // prizeType, userId

    @DynamoDbPartitionKey
    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Map<String, String> getContents() {
        return contents;
    }

    public void setContents(Map<String, String> contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "EventCustomData{" +
               "primaryKey='" + primaryKey + '\'' +
               ", contents=" + contents +
               '}';
    }
}
