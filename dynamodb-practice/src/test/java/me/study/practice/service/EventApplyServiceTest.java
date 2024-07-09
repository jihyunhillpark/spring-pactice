package me.study.practice.service;

import me.study.practice.config.DynamoDBConfig;
import me.study.practice.domain.EventCustomData;
import me.study.practice.domain.PrizeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/**
 * DynamoDB의 연결&동작을 확인하기 위한 Integration Test
 */
@SpringBootTest
@Import(DynamoDBConfig.class)
class EventApplyServiceTest {
    private static final long MAX_APPLICANTS = 1000L;
    private static final String TEST_EVENT_PK = "20240711-test";
    private static final String TEST_EVENT_PK2 = "20240811-test";
    private DynamoDbTable<EventCustomData> eventDataTable;
    @Autowired
    private EventApplyService eventApplyService;

    @BeforeEach
    void setUp() {
        eventDataTable = initiateTable();
        eventDataTable.putItem(EventCustomData.builder()
                                              .primaryKey(TEST_EVENT_PK)
                                              .contents(Map.of("limit", "1",
                                                               "stock", String.valueOf(MAX_APPLICANTS)))
                                              .build());
        eventDataTable.putItem(EventCustomData.builder()
                                              .primaryKey(TEST_EVENT_PK2)
                                              .contents(Map.of("limit", "1",
                                                               "stock", String.valueOf(MAX_APPLICANTS)))
                                              .build());
    }

    @Test
    @DisplayName("이벤트 신청 - 재고차감 후 바로 경품 지급")
    void apply() {
        eventApplyService.apply(TEST_EVENT_PK, "1234567", PrizeType.COUPON);
        final QueryConditional userQueryConditional = QueryConditional
            .keyEqualTo(k -> k.partitionValue("1234567"));
        final QueryConditional eventConditional = QueryConditional
            .keyEqualTo(k -> k.partitionValue(TEST_EVENT_PK));

        final DynamoDbTable<EventCustomData> eventCustomDataTable = eventApplyService.getEventCustomDataTable();
        eventCustomDataTable.query(r -> r.queryConditional(userQueryConditional))
                            .items()
                            .stream()
                            .findAny()
                            .ifPresent(eventCustomData -> {
                                final Map<String, String> contents = eventCustomData.getContents();
                                assertThat(contents.get("eventId")).isEqualTo(TEST_EVENT_PK);
                                assertThat(contents.get("prizeType")).isEqualTo(PrizeType.COUPON.name());
                            });
        eventCustomDataTable.query(r -> r.queryConditional(eventConditional))
                            .items()
                            .stream()
                            .findAny()
                            .ifPresent(eventCustomData -> {
                                final Map<String, String> contents = eventCustomData.getContents();
                                assertThat(contents.get("stock")).isEqualTo("999");
                            });
    }

    @Test
    @DisplayName("apply - 중복 신청 실패")
    void duplicateApply_fails() {
        eventApplyService.apply(TEST_EVENT_PK, "1234567", PrizeType.COUPON);
        assertThatThrownBy(() -> eventApplyService.apply(TEST_EVENT_PK, "1234567", PrizeType.COUPON))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("이벤트 신청자 횟수를 초과했습니다.");
    }

    @Test
    @DisplayName("특정 유저의 이벤트 응모내역 조회")
    void getUserApplyHistory() {
        eventApplyService.apply(TEST_EVENT_PK, "1234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK2, "1234567", PrizeType.POINT);

        eventApplyService.getUserApplyHistory(TEST_EVENT_PK, "1234567")
                         .stream()
                         .findFirst()
                         .ifPresent(eventCustomData -> {
                             final Map<String, String> contents = eventCustomData.getContents();
                             assertThat(contents.get("eventId")).isEqualTo(TEST_EVENT_PK);
                             assertThat(contents.get("prizeType")).isEqualTo(PrizeType.COUPON.name());
                         });

    }

    @Test
    @DisplayName("포인트 받은 유저 모두 조회")
    void getPointPrizedUsers() {
        eventApplyService.apply(TEST_EVENT_PK, "1234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "2234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "3234567", PrizeType.POINT);
        eventApplyService.apply(TEST_EVENT_PK, "4234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "5234567", PrizeType.POINT);
        eventApplyService.apply(TEST_EVENT_PK, "6234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "7234567", PrizeType.POINT);

        assertThatList(eventApplyService.getPrizeSuppliedUsers(TEST_EVENT_PK, PrizeType.POINT.name()))
            .contains("3234567", "5234567", "7234567");
    }

    @Test
    @DisplayName("포인트 받은 유저 모두 조회")
    final void getCouponPrizedUsers() {
        eventApplyService.apply(TEST_EVENT_PK, "1234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "2234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "3234567", PrizeType.POINT);
        eventApplyService.apply(TEST_EVENT_PK, "4234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "5234567", PrizeType.POINT);
        eventApplyService.apply(TEST_EVENT_PK, "6234567", PrizeType.COUPON);
        eventApplyService.apply(TEST_EVENT_PK, "7234567", PrizeType.POINT);

        assertThatList(eventApplyService.getPrizeSuppliedUsers(TEST_EVENT_PK, PrizeType.COUPON.name()))
            .contains("1234567", "2234567", "4234567", "6234567");
    }

    private DynamoDbTable<EventCustomData> initiateTable() {
        final CreateTableEnhancedRequest request = CreateTableEnhancedRequest.builder()
                                                                             .provisionedThroughput(ProvisionedThroughput
                                                                                                        .builder()
                                                                                                        .readCapacityUnits(MAX_APPLICANTS)
                                                                                                        .writeCapacityUnits(MAX_APPLICANTS)
                                                                                                        .build())
                                                                             .build();
        final DynamoDbTable<EventCustomData> eventCustomDataTable = eventApplyService.getEventCustomDataTable();
        if (null != eventCustomDataTable) eventCustomDataTable.deleteTable();
        eventCustomDataTable.createTable(request);
        System.out.printf("Tables created: %s", eventCustomDataTable.tableName());
        return eventCustomDataTable;
    }
}