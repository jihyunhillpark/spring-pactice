package me.study.practice.service;

import lombok.RequiredArgsConstructor;
import me.study.practice.domain.AttributesDataType;
import me.study.practice.domain.EventApply;
import me.study.practice.domain.Prize;
import me.study.practice.domain.PrizeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventApplyService {
    private static final String DATA_TYPE_APPLY = AttributesDataType.EVENT_APPLY.name();
    private static final int MAX_APPLICANTS = 1000;
    private static final String TABLE_NAME = "other-data";
    private final DynamoDbEnhancedClient dynamoDbClient;
    private final DynamoDbTable<EventApply> eventApply;
    private final DynamoDbTable<Prize> prize;

    @Autowired
    EventApplyService(DynamoDbEnhancedClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.eventApply = dynamoDbClient.table(TABLE_NAME, TableSchema.fromBean(EventApply.class));
        this.prize = dynamoDbClient.table(TABLE_NAME, TableSchema.fromBean(Prize.class));
    }

    private void createTable() {
        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                       .provisionedThroughput(software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
                                                                                                  .builder()
                                                                                                  .readCapacityUnits(10L)
                                                                                                  .writeCapacityUnits(10L)
                                                                                                  .build())
                                                                       .build();

        eventApply.createTable(request);
        prize.createTable(request);
        System.out.println("Tables created: " + TABLE_NAME);
    }

    public void apply(String eventId, String userId, PrizeType prizeType) {
        if (getEventApplyCount() >= MAX_APPLICANTS) {
            System.out.println("EventApply limit reached");
            return;
        }

        if (isApplied(userId)) {
            System.out.println("User has already applied");
            return;
        }

        EventApply eventApply = EventApply.builder()
                                          .eventId(eventId)
                                          .userId(userId)
                                          .prizeType(prizeType.name())
                                          .build();

        final UpdateItemEnhancedResponse response = decreaseStock(eventId, prizeType);
        if (response != null) {
            System.out.println("Decreased stock for " + eventId);
            this.eventApply.putItem(eventApply);
        }
        System.out.println("User " + userId + " applied and received " + prizeType);
    }

    public List<EventApply> getUserApplyHistory(String eventId, String userId) {
        QueryConditional keyConditional = QueryConditional
            .keyEqualTo(k -> k.partitionValue(DATA_TYPE_APPLY).sortValue(eventId));
        final Expression filterUserConditional = Expression.builder()
                                                           .expression("userId = :userId")
                                                           .expressionValues(Map.of(":userId", AttributeValue.builder().s(userId).build()))
                                                           .build();

        QueryEnhancedRequest queryConditionals = QueryEnhancedRequest.builder()
                                                                     .consistentRead(true) // 이걸로 동시성이 해결되려나?
                                                                     .queryConditional(keyConditional)
                                                                     .filterExpression(filterUserConditional)
                                                                     .build();

        return this.eventApply.query(queryConditionals)
                              .items()
                              .stream()
                              .collect(Collectors.toUnmodifiableList());
    }

    public List<String> getPrizeSuppliedUsers(String eventId, String userId, String prizeType) {
        Map<String, AttributeValue> exclusiveStartKeys = Map.of(
            ":dataType", AttributeValue.builder().s(AttributesDataType.PRIZE.name()).build(),
            ":eventId", AttributeValue.builder().s(userId).build());
        Map<String, AttributeValue> expressionValues = Map.of(
            ":userId", AttributeValue.builder().s(eventId).build(),
            ":prizeType", AttributeValue.builder().s(prizeType).build());
        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                                                             .consistentRead(true)
                                                             .attributesToProject("dataType", "eventId", "userId", "prizeType")
                                                             .exclusiveStartKey(exclusiveStartKeys)
                                                             .filterExpression(Expression.builder()
                                                                                         .expression(
                                                                                             "userId = :userId and prizeType = :prizeType")
                                                                                         .expressionValues(expressionValues)
                                                                                         .build())
                                                             .build();

        return eventApply.scan(scanRequest)
                         .items()
                         .stream()
                         .map(EventApply::getUserId)
                         .collect(Collectors.toUnmodifiableList());
    }

    private boolean isApplied(String userId) {
        QueryConditional queryConditional = QueryConditional
            .keyEqualTo(k -> k.partitionValue(DATA_TYPE_APPLY).sortValue(userId));

        return eventApply.query(r -> r.queryConditional(queryConditional))
                         .items()
                         .stream()
                         .findFirst()
                         .isPresent();
    }

    private int getEventApplyCount() {
        return (int) eventApply.scan().items().stream().count();
    }

    public UpdateItemEnhancedResponse decreaseStock(final String eventId, final PrizeType prizeType) {
        final UpdateItemEnhancedRequest<Prize> decreaseStockRequest = UpdateItemEnhancedRequest.builder(Prize.class)
                                                                                               .item(this.prize
                                                                                                         .getItem(Key.builder()
                                                                                                                     .partitionValue(
                                                                                                                         AttributesDataType.PRIZE.name())
                                                                                                                     .sortValue(eventId)
                                                                                                                     .build())
                                                                                                         .decreaseStock())
                                                                                               .conditionExpression(Expression.builder()
                                                                                                                              .expression(
                                                                                                                                  "#stock > :zero")
                                                                                                                              .build())
                                                                                               .ignoreNulls(true)
                                                                                               .build();
        return this.prize.updateItemWithResponse(decreaseStockRequest); // 상품 재고 감소
    }
}
