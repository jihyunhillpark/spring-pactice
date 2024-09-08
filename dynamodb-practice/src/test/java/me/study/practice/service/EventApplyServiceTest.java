package me.study.practice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import me.study.practice.config.DynamoDBConfig;
import me.study.practice.domain.Event;
import me.study.practice.domain.EventApply;
import me.study.practice.domain.Reward;
import me.study.practice.domain.SuppliedReward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * DynamoDB의 연결&동작을 확인하기 위한 Integration Test
 */
@SpringBootTest
@Import(DynamoDBConfig.class)
class EventApplyServiceTest {
    private static final long MAX_APPLICANTS = 1000L;
    private static final String TEST_EVENT_PK = "event-id:20240711-test";
    private static final String TEST_EVENT_PK2 = "event-id:20240811-test";
    private static final String TEST_EVENT_SORT_KEY = "event-term:1";
    private static final String TEST_REWARD_SORT_KEY = "reward-id:COUPON";
    private static final String TEST_REWARD_SORT_KEY2 = "reward-id:POINT";
    private DynamoDbTable<EventApply> eventDataTable;
    @Autowired
    private EventApplyService eventApplyService;
    @Autowired
    private DynamoDbEnhancedClient enhancedClient;
    private DynamoDbTable<EventApply> eventApplyTable;
    private DynamoDbTable<Event> eventTable;
    private DynamoDbTable<Reward> rewardTable;
    private DynamoDbTable<SuppliedReward> suppliedRewardTable;
    @Autowired
    private DynamoDbClient dynamoDbClient;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        eventApplyTable = initiateTable("event-data", EventApply.class);
        eventTable = initiateTable("event-data", Event.class);
        rewardTable = initiateTable("event-data", Reward.class);
        suppliedRewardTable = initiateTable("event-data", SuppliedReward.class);

        createTestEventIfNotExists();
        createTestRewardIfNotExists();
    }


    @Test
    @DisplayName("이벤트 신청 - 재고차감 후 바로 경품 지급")
    void apply() {
        boolean result = eventApplyService.apply("20240711-test", "123456", "COUPON");

        // Assert
        assertThat(result).isTrue();
        // Verify that the EventApply item was created
        final Key eventApplyKey = Key.builder()
                                     .partitionValue("user-id:123456")
                                     .sortValue(TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY)
                                     .build();
        EventApply eventApply = eventApplyTable.getItem(eventApplyKey);
        assertThat(eventApply).isNotNull();
        assertThat(eventApply.getApplyStatus()).isEqualTo("APPLIED");

        // Verify that the stock count was decremented
        final Key rewardKey = Key.builder()
                                 .partitionValue(TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY)
                                 .sortValue("reward-id:COUPON")
                                 .build();
        Reward reward = rewardTable.getItem(rewardKey);
        assertThat(reward.getStockCount()).isEqualTo(999); // Assuming the initial stock was 1000

        final Key suppliedRewardKey = Key.builder()
                                         .partitionValue(TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY + ";reward-id:COUPON")
                                         .sortValue("user-id:123456")
                                         .build();
        SuppliedReward suppliedReward = suppliedRewardTable.getItem(suppliedRewardKey);
        assertThat(suppliedReward).isNotNull();
    }

    @Test
    @DisplayName("apply - 중복 신청 실패")
    void duplicateApply_fails() {
        // Act
        boolean result = eventApplyService.apply("20240711-test", "123456", "COUPON");
        boolean duplicateResult = eventApplyService.apply("20240711-test", "123456", "COUPON");

        // Assert
        assertThat(duplicateResult).isFalse();
    }

    @Test
    @DisplayName("1001 번째 고객은 신청 실패")
    void apply1001stUser_fails() throws InterruptedException {
        // Given
        final int numberOfThreads = 1100;

        try (ExecutorService executorService = Executors.newFixedThreadPool(100)) {
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            // Act
            for (long i = 0; i < numberOfThreads; i++) {
                long userId = i + 1;
                executorService.submit(() -> {
                    try {
                        eventApplyService.apply("20240711-test", "user-id:" + userId, "COUPON");
                    } catch (Exception e) {
                        // Ignore exceptions
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            executorService.shutdown();
        }
        // Then
        String partitionKeyValue = TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY;
        String suppliedRewardKey = TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY + ";reward-id:COUPON";
        final Key rewardKey = Key.builder()
                                 .partitionValue(partitionKeyValue)
                                 .sortValue("reward-id:COUPON")
                                 .build();
        Reward reward = rewardTable.getItem(rewardKey);
        assertThat(reward.getStockCount()).isEqualTo(0);

        // Query 요청 생성
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":v1", AttributeValue.builder().s(suppliedRewardKey).build());

        QueryRequest queryRequest = QueryRequest.builder()
                                                .tableName("event-data") // 테이블 이름
                                                .keyConditionExpression("partitionKey = :v1") // 키 조건 표현식
                                                .expressionAttributeValues(expressionValues)
                                                .build();

        // Query 실행
        QueryResponse response = dynamoDbClient.query(queryRequest);

        // 항목 개수 출력
        int itemCount = response.count();
        assertThat(itemCount).isEqualTo(1000);
    }

    @Test
    @DisplayName("특정 유저의 이벤트 응모내역 조회")
    void getUserApplyHistory() {
        // Act
        boolean result = eventApplyService.apply("20240711-test", "123456", "COUPON");
        boolean result2 = eventApplyService.apply("20240811-test", "123456", "COUPON");
        List<EventApply> applyHistory = eventApplyService.getUserApplyHistory("123456");

        // Assert
        assertThat(applyHistory).hasSize(2);
    }

    @Test
    @DisplayName("쿠폰 받은 유저 모두 조회")
    void getCouponPrizedUsers() {
        boolean result = eventApplyService.apply("20240711-test", "123456", "COUPON");
        boolean result2 = eventApplyService.apply("20240711-test", "123457", "COUPON");
        boolean result3 = eventApplyService.apply("20240711-test", "123458", "COUPON");
        boolean result4 = eventApplyService.apply("20240711-test", "111111", "POINT");
        List<String> users = eventApplyService.getRewardSuppliedUsers("20240711-test", "COUPON");
        assertThat(users).hasSize(3);
    }


    @Test
    @DisplayName("포인트 받은 유저 모두 조회")
    void getPointPrizedUsers() {
        boolean result = eventApplyService.apply("20240711-test", "123456", "COUPON");
        boolean result2 = eventApplyService.apply("20240711-test", "123457", "COUPON");
        boolean result3 = eventApplyService.apply("20240711-test", "123458", "COUPON");
        boolean result4 = eventApplyService.apply("20240711-test", "111111", "POINT");
        List<String> users = eventApplyService.getRewardSuppliedUsers("20240711-test", "POINT");
        assertThat(users).hasSize(1);
    }

    private <T> DynamoDbTable<T> initiateTable(String tableName, Class<T> clazz) {
        DynamoDbTable<T> table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));

        // Drop the table if it exists
        try {
            table.deleteTable();
        } catch (Exception e) {
            // Ignore if table does not exist or other errors
        }

        // Recreate the table
        CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                       .provisionedThroughput(ProvisionedThroughput.builder()
                                                                                                                   .readCapacityUnits(
                                                                                                                       MAX_APPLICANTS)
                                                                                                                   .writeCapacityUnits(
                                                                                                                       MAX_APPLICANTS)
                                                                                                                   .build())
                                                                       .build();

        table.createTable(request);
        return table;
    }

    private void createTestEventIfNotExists() throws JsonProcessingException {
        // Create test event if it does not exist
        Event testEvent = Event.builder()
                               .partitionKey(TEST_EVENT_PK)
                               .sortKey(TEST_EVENT_SORT_KEY)
                               .rewardIds(List.of(TEST_REWARD_SORT_KEY))
                               .startAt(LocalDateTime.now().minusDays(1))
                               .endAt(LocalDateTime.now().plusDays(1))
                               .createdAt(LocalDateTime.now())
                               .updatedAt(LocalDateTime.now())
                               .build();
        Event testEvent2 = Event.builder()
                                .partitionKey(TEST_EVENT_PK2)
                                .sortKey(TEST_EVENT_SORT_KEY)
                                .rewardIds(List.of(TEST_REWARD_SORT_KEY))
                                .startAt(LocalDateTime.now().minusDays(1))
                                .endAt(LocalDateTime.now().plusDays(1))
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

        eventTable.putItem(testEvent);
        eventTable.putItem(testEvent2);
    }

    private void createTestRewardIfNotExists() {
        // Create test reward if it does not exist
        Reward testReward = Reward.builder()
                                  .partitionKey(TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY)
                                  .sortKey(TEST_REWARD_SORT_KEY2)
                                  .name("Test Reward")
                                  .rewardType("POINT")
                                  .eventId(TEST_EVENT_PK)
                                  .stockCount(1000)
                                  .createdAt(LocalDateTime.now())
                                  .updatedAt(LocalDateTime.now())
                                  .validFrom(LocalDateTime.now().minusDays(1))
                                  .validTo(LocalDateTime.now().plusDays(1))
                                  .build();
        Reward testReward2 = Reward.builder()
                                   .partitionKey(TEST_EVENT_PK2 + ";" + TEST_EVENT_SORT_KEY)
                                   .sortKey(TEST_REWARD_SORT_KEY2)
                                   .name("Test Reward")
                                   .rewardType("POINT")
                                   .eventId(TEST_EVENT_PK)
                                   .stockCount(1000)
                                   .createdAt(LocalDateTime.now())
                                   .updatedAt(LocalDateTime.now())
                                   .validFrom(LocalDateTime.now().minusDays(1))
                                   .validTo(LocalDateTime.now().plusDays(1))
                                   .build();
        Reward testReward3 = Reward.builder()
                                   .partitionKey(TEST_EVENT_PK + ";" + TEST_EVENT_SORT_KEY)
                                   .sortKey(TEST_REWARD_SORT_KEY)
                                   .name("Test Reward")
                                   .rewardType("COUPON")
                                   .eventId(TEST_EVENT_PK)
                                   .stockCount(1000)
                                   .createdAt(LocalDateTime.now())
                                   .updatedAt(LocalDateTime.now())
                                   .validFrom(LocalDateTime.now().minusDays(1))
                                   .validTo(LocalDateTime.now().plusDays(1))
                                   .build();
        Reward testReward4 = Reward.builder()
                                   .partitionKey(TEST_EVENT_PK2 + ";" + TEST_EVENT_SORT_KEY)
                                   .sortKey(TEST_REWARD_SORT_KEY)
                                   .name("Test Reward")
                                   .rewardType("COUPON")
                                   .eventId(TEST_EVENT_PK)
                                   .stockCount(1000)
                                   .createdAt(LocalDateTime.now())
                                   .updatedAt(LocalDateTime.now())
                                   .validFrom(LocalDateTime.now().minusDays(1))
                                   .validTo(LocalDateTime.now().plusDays(1))
                                   .build();
        rewardTable.putItem(testReward);
        rewardTable.putItem(testReward2);
        rewardTable.putItem(testReward3);
        rewardTable.putItem(testReward4);
    }

}