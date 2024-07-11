package me.study.practice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.study.practice.domain.EventCustomData;
import me.study.practice.domain.PrizeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventApplyService {
    private static final String TABLE_NAME = "other-data";
    private final DynamoDbEnhancedClient dynamoDbClient;
    private final DynamoDbTable<EventCustomData> eventCustomDataTable;

    @Autowired
    EventApplyService(final DynamoDbEnhancedClient dynamoDbClient) {
        this.dynamoDbClient = dynamoDbClient;
        this.eventCustomDataTable = dynamoDbClient.table(TABLE_NAME, TableSchema.fromBean(EventCustomData.class));
    }

    DynamoDbTable<EventCustomData> getEventCustomDataTable() {
        return eventCustomDataTable;
    }

    public void apply(final String eventId, final String memberNumber, final PrizeType prizeType) {
        validateApply(memberNumber, eventId);
        final EventCustomData keyItem = EventCustomData.builder()
                                                       .primaryKey(eventId)
                                                       .build();
        // 업데이트를 위한 항목을 로드
        final EventCustomData existingItem = eventCustomDataTable.getItem(keyItem);
        final Map<String, String> contents = existingItem.getContents();

        // stock 값을 가져와 0 이상인지 확인하고 1 감소
        int stock = Integer.parseInt(contents.get("stock"));
        if (0 < stock) {
            stock--;
            contents.put("stock", String.valueOf(stock));
        } else {
            // 조건에 맞지 않으면 반환
            throw new IllegalArgumentException("경품 재고가 부족합니다.");
        }
        existingItem.setContents(contents);

        // 업데이트 요청
        final UpdateItemEnhancedRequest<EventCustomData> request = UpdateItemEnhancedRequest.builder(EventCustomData.class)
                                                                                            .item(existingItem)
                                                                                            .build();
        // user 파티션키 생성
        final PutItemEnhancedRequest<EventCustomData> request2 = PutItemEnhancedRequest.builder(EventCustomData.class)
                                                                                       .item(EventCustomData.builder()
                                                                                                            .primaryKey(memberNumber)
                                                                                                            .contents(Map.of("eventId",
                                                                                                                             eventId,
                                                                                                                             "prizeType",
                                                                                                                             prizeType.name()))
                                                                                                            .build())
                                                                                       .build();

        // 업데이트 수행
        try {
            dynamoDbClient.transactWriteItems(r -> r.addUpdateItem(eventCustomDataTable, request)
                                                    .addPutItem(eventCustomDataTable, request2));
        } catch (final DynamoDbException e) {
            log.error(e.getMessage());
        }
        log.info("apply success : eventId [ %s ] contents was updated!", eventId);
    }

    public List<EventCustomData> getUserApplyHistory(final String eventId, final String memberNumber) {
        final QueryEnhancedRequest queryConditionals = getUserApplyHistoryQuery(eventId,
                                                                                memberNumber);

        return eventCustomDataTable.query(queryConditionals)
                                   .items()
                                   .stream()
                                   .toList();
    }

    public List<String> getPrizeSuppliedUsers(final String eventId, final String prizeType) {
        final Expression filterConditional = Expression.builder()
                                                       .expression("contents.eventId = :eventId and contents.prizeType = :prizeType")
                                                       .expressionValues(Map.of(":eventId",
                                                                                AttributeValue.builder().s(eventId).build(),
                                                                                ":prizeType",
                                                                                AttributeValue.builder().s(prizeType).build()))
                                                       .build();
        final ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                                                                   .consistentRead(true)
                                                                   .attributesToProject("primaryKey", "contents")
                                                                   .filterExpression(filterConditional)
                                                                   .build();

        return eventCustomDataTable.scan(scanRequest)
                                   .items()
                                   .stream()
                                   .map(EventCustomData::getPrimaryKey)
                                   .toList();
    }

    private QueryEnhancedRequest getUserApplyHistoryQuery(final String eventId, final String memberNumber) {
        final QueryConditional keyConditional = QueryConditional
            .keyEqualTo(k -> k.partitionValue(memberNumber));
        final Expression userConditional = Expression.builder()
                                                     .expression("contents.eventId = :eventId")
                                                     .expressionValues(Map.of(":eventId",
                                                                              AttributeValue.builder().s(eventId).build()))
                                                     .build();

        return QueryEnhancedRequest.builder()
                                   .consistentRead(true) // 이걸로 동시성이 해결되려나?
                                   .queryConditional(keyConditional)
                                   .filterExpression(userConditional)
                                   .build();
    }

    private void validateApply(final String memberNumber, final String eventId) {
        final QueryConditional eventKeyConditional = QueryConditional
            .keyEqualTo(key -> key.partitionValue(eventId));
        final QueryEnhancedRequest queryConditionals = getUserApplyHistoryQuery(eventId, memberNumber);

        final long applyCount = eventCustomDataTable.query(queryConditionals)
                                                    .items()
                                                    .stream()
                                                    .count();

        eventCustomDataTable.query(r -> r.queryConditional(eventKeyConditional))
                            .items()
                            .stream()
                            .findAny()
                            .ifPresent(eventCustomData -> {
                                final Map<String, String> contents = eventCustomData.getContents();
                                final long applyLimit = Long.parseLong(contents.get("limit"));
                                if (applyLimit <= applyCount) {
                                    throw new IllegalArgumentException("이벤트 응모 가능 횟수를 초과했습니다.");
                                }
                                if (0 >= Integer.parseInt(contents.get("stock"))) {
                                    throw new IllegalArgumentException("경품 재고가 부족합니다.");
                                }
                            });
    }
}
