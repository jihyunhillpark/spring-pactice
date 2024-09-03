package me.study.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.study.practice.domain.Event;
import me.study.practice.domain.EventApply;
import me.study.practice.domain.Reward;
import me.study.practice.domain.SuppliedReward;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventApplyService {
    private static final String TABLE_NAME = "event-data";
    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbTable<Event> eventTable;
    private final DynamoDbTable<Reward> rewardTable;
    private final DynamoDbTable<SuppliedReward> suppliedRewardTable;
    private final DynamoDbTable<EventApply> eventApplyTable;

    @Autowired
    EventApplyService(final DynamoDbEnhancedClient enhancedClient, final DynamoDbClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.eventTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Event.class));
        this.rewardTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(Reward.class));
        this.suppliedRewardTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(SuppliedReward.class));
        this.eventApplyTable = enhancedClient.table(TABLE_NAME, TableSchema.fromBean(EventApply.class));
    }

    DynamoDbTable<EventApply> getEventApplyDynamoDbTable() {
        return eventApplyTable;
    }

    public boolean apply(String eventId, String userId, String rewardId) {
        final String eventKey = "event-id:" + eventId;
        final String eventSortKey = "event-term:1";
        final String eventApplyPrimaryKey = "user-id:" + userId;
        final String eventApplySortKey = eventKey + ";" + eventSortKey;
        final String rewardSortKey = "reward-id:" + rewardId;
        try {
            // 1. Event와 Reward 조회
            Event event = eventTable.getItem(Key.builder().partitionValue(eventKey).sortValue(eventSortKey).build());
            Reward reward = rewardTable.getItem(Key.builder().partitionValue(eventApplySortKey).sortValue(rewardSortKey).build());

            if (event == null || reward == null || reward.getStockCount() <= 0) {
                System.out.println("Event or Reward not found, or Reward out of stock");
                return false;
            }

            // 2. 기존 EventApply가 존재하는지 확인
            EventApply existingApply = eventApplyTable.getItem(Key.builder()
                                                                  .partitionValue(eventApplyPrimaryKey)
                                                                  .sortValue(eventApplySortKey)
                                                                  .build());

            if (existingApply != null) {
                System.out.println("User has already applied for this event with this reward");
                return false;
            }

            // 2. TransactWriteItems를 사용하여 원자적 작업 처리
            EventApply eventApply = EventApply.builder()
                                              .partitionKey(eventApplyPrimaryKey)
                                              .sortKey(eventApplySortKey)
                                              .applyStatus("APPLIED")
                                              .createdAt(LocalDateTime.now())
                                              .updatedAt(LocalDateTime.now())
                                              .build();

            SuppliedReward suppliedReward = SuppliedReward.builder()
                                                          .partitionKey(createSuppliedRewardPartitionKey(eventId, rewardId))
                                                          .sortKey(eventApplyPrimaryKey)
                                                          .status("SUPPLIED")
                                                          .build();

            TransactWriteItemsRequest transactWriteRequest = TransactWriteItemsRequest.builder()
                                                                                      .transactItems(
                                                                                          Arrays.asList(
                                                                                              TransactWriteItem.builder()
                                                                                                               .put(Put.builder()
                                                                                                                       .tableName(TABLE_NAME)
                                                                                                                       .item(eventApplyTable
                                                                                                                                 .tableSchema()
                                                                                                                                 .itemToMap(
                                                                                                                                     eventApply,
                                                                                                                                     true))
                                                                                                                       .conditionExpression(
                                                                                                                           "attribute_not_exists(#pk) AND attribute_not_exists(#sk)")
                                                                                                                       .expressionAttributeNames(
                                                                                                                           Map.of("#pk",
                                                                                                                                  "primaryKey",
                                                                                                                                  "#sk",
                                                                                                                                  "sortKey"))
                                                                                                                       .build())
                                                                                                               .build(),
                                                                                              TransactWriteItem.builder()
                                                                                                               .update(Update.builder()
                                                                                                                             .tableName(
                                                                                                                                 TABLE_NAME)
                                                                                                                             .key(
                                                                                                                                 createRewardKey(
                                                                                                                                     eventApplySortKey,
                                                                                                                                     rewardSortKey))
                                                                                                                             .updateExpression(
                                                                                                                                 "SET stockCount = stockCount - :decrement")
                                                                                                                             .conditionExpression(
                                                                                                                                 "stockCount > :zero")
                                                                                                                             .expressionAttributeValues(
                                                                                                                                 Map.of(
                                                                                                                                     ":decrement",
                                                                                                                                     AttributeValue
                                                                                                                                         .builder()
                                                                                                                                         .n("1")
                                                                                                                                         .build(),
                                                                                                                                     ":zero",
                                                                                                                                     AttributeValue
                                                                                                                                         .builder()
                                                                                                                                         .n("0")
                                                                                                                                         .build()))
                                                                                                                             .build())
                                                                                                               .build(),
                                                                                              TransactWriteItem.builder()
                                                                                                               .put(Put.builder()
                                                                                                                       .tableName(TABLE_NAME)
                                                                                                                       .item(
                                                                                                                           suppliedRewardTable
                                                                                                                               .tableSchema()
                                                                                                                               .itemToMap(
                                                                                                                                   suppliedReward,
                                                                                                                                   true))
                                                                                                                       .conditionExpression(
                                                                                                                           "attribute_not_exists(#pk) AND attribute_not_exists(#sk)")
                                                                                                                       .expressionAttributeNames(
                                                                                                                           Map.of("#pk",
                                                                                                                                  "primaryKey",
                                                                                                                                  "#sk",
                                                                                                                                  "sortKey"))
                                                                                                                       .build())
                                                                                                               .build())
                                                                                                    ).build();

            dynamoDbClient.transactWriteItems(transactWriteRequest);

            return true;
        } catch (TransactionCanceledException e) {
            System.out.println("Transaction failed: " + e.getMessage());
            return false;
        }
    }

    private Map<String, AttributeValue> createRewardKey(String eventId, String rewardId) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("partitionKey", AttributeValue.builder().s(eventId).build());
        key.put("sortKey", AttributeValue.builder().s(rewardId).build());
        return key;
    }

    public List<EventApply> getUserApplyHistory(final String userId) {
        var queryConditional = QueryConditional
            .keyEqualTo(Key.builder()
                           .partitionValue("user-id:" + userId)
                           .build());

        var results = eventApplyTable.query(r -> r.queryConditional(queryConditional)).items();

        return StreamSupport.stream(results.spliterator(), false)
                            .collect(Collectors.toList());
    }

    public List<String> getRewardSuppliedUsers(final String eventId, final String rewardId) {
        // Create the primary key and sort key for the query
        String partitionKey = createSuppliedRewardPartitionKey(eventId, rewardId);

        // Build the query condition
        QueryConditional queryConditional = QueryConditional.keyEqualTo(Key.builder()
                                                                           .partitionValue(partitionKey)
                                                                           .build());

        // Execute the query
        QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                                                                .queryConditional(queryConditional)
                                                                .build();

        PageIterable<SuppliedReward> queryResponse = suppliedRewardTable.query(queryRequest);

        // Extract userIds from the results
        return queryResponse.items().stream()
                            .map(item -> item.getSortKey().split(" ;")[0].replace("user-id:", ""))
                            .collect(Collectors.toList());
    }

    private String createSuppliedRewardPartitionKey(String eventId, String rewardId) {
        return "event-id:" + eventId + ";event-term:1;reward-id:" + rewardId;
    }
}
