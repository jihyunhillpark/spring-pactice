package me.study.practice.compare.service;

import me.study.practice.compare.mariadb.controller.EventApplyRequest;
import me.study.practice.compare.mariadb.domain.EventApply;
import me.study.practice.compare.mariadb.domain.Reward;
import me.study.practice.compare.mariadb.repository.EventApplyRepository;
import me.study.practice.compare.mariadb.repository.RewardRepository;
import me.study.practice.compare.mariadb.service.MariaEventApplyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class MariaEventApplyServiceTest {

    private static final String TEST_EVENT_ID = "test-20240816";
    private static final String TEST_EVENT_ID_2 = "test-20240916";

    @Autowired
    private MariaEventApplyService mariaEventApplyService;

    @Autowired
    private EventApplyRepository eventApplyRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @BeforeEach
    void setUp() {
        eventApplyRepository.deleteAll();
        rewardRepository.deleteAll();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Reward createAndSaveReward(final String eventId, final String rewardType) {
        Reward reward = new Reward();
        reward.setId("reward-" + eventId + "-" + rewardType);
        reward.setName("Test Reward " + rewardType);
        reward.setRewardType(rewardType);
        reward.setEventId(eventId);
        reward.setStockCount(1000);
        Reward savedReward = rewardRepository.save(reward);
        rewardRepository.flush(); // 데이터베이스에 즉시 반영
        return savedReward;
    }

    private EventApplyRequest createEventApply(final String eventId, final long userId, final String rewardType) {
        EventApplyRequest eventApply = new EventApplyRequest();
        eventApply.setEventId(eventId);
        eventApply.setUserId(userId);
        eventApply.setRewardType(rewardType);
        eventApply.setApplyStatus("REWARD_PROVIDED");
        return eventApply;
    }

    @Test
    public void test1_1000ApplicationsLimit() throws InterruptedException {
        // Given
        final Reward reward = createAndSaveReward(TEST_EVENT_ID, "POINT");
        final int numberOfThreads = 1100;

        try (ExecutorService executorService = Executors.newFixedThreadPool(100)) {
            CountDownLatch latch = new CountDownLatch(numberOfThreads);

            // When
            for (long i = 0; i < numberOfThreads; i++) {
                long userId = i + 1;
                executorService.submit(() -> {
                    try {
                        EventApplyRequest eventApply = createEventApply(TEST_EVENT_ID, userId, reward.getRewardType());
                        mariaEventApplyService.applyToEvent(eventApply);
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
        final long applicationCount = eventApplyRepository.countByEventId(TEST_EVENT_ID);
        assertEquals(1000, applicationCount);

        Reward updatedReward = rewardRepository.findById(reward.getId()).get();
        assertEquals(0, updatedReward.getStockCount());
    }

    @Test
    @Transactional
    public void test2_ApplyWhenNoApplicationExists() {
        // Given
        final Reward reward = createAndSaveReward(TEST_EVENT_ID, "POINT");
        System.out.println("REWARD : " + rewardRepository.findById(reward.getId()).get().getEventId());
        final EventApplyRequest eventApply = createEventApply(TEST_EVENT_ID, 1L, reward.getRewardType());

        // When
        final EventApply savedEventApply = mariaEventApplyService.applyToEvent(eventApply);

        // Then
        assertNotNull(savedEventApply);
        assertEquals("REWARD_PROVIDED", savedEventApply.getApplyStatus());
    }

    @Test
    @Transactional
    public void test3_FailWhenApplicationExists() {
        // Given
        final Reward reward = createAndSaveReward(TEST_EVENT_ID, "POINT");
        final EventApplyRequest eventApply = createEventApply(TEST_EVENT_ID, 1L, reward.getRewardType());
        mariaEventApplyService.applyToEvent(eventApply); // Apply once to set up state

        // When & Then
        assertThrows(RuntimeException.class, () -> mariaEventApplyService.applyToEvent(eventApply));
    }

    @Test
    @Transactional
    public void test4_GetAllApplicationsForUser() {
        // Given
        final Reward reward1 = createAndSaveReward(TEST_EVENT_ID, "POINT");
        final EventApplyRequest eventApply1 = createEventApply(TEST_EVENT_ID, 1L, reward1.getRewardType());
        final Reward reward2 = createAndSaveReward(TEST_EVENT_ID_2, "COUPON");
        final EventApplyRequest eventApply2 = createEventApply(TEST_EVENT_ID_2, 1L, reward2.getRewardType());

        mariaEventApplyService.applyToEvent(eventApply1);
        mariaEventApplyService.applyToEvent(eventApply2);

        // When
        final List<EventApply> applications = mariaEventApplyService.getUserApplications(1L);

        // Then
        assertEquals(2, applications.size());
    }

    @Test
    @Transactional
    public void test5_GetUsersWithPointRewards() {
        // Given
        final Reward reward = createAndSaveReward(TEST_EVENT_ID, "POINT");
        for (long i = 1L; 10L >= i; i++) {
            final EventApplyRequest eventApply = createEventApply(TEST_EVENT_ID, i, reward.getRewardType());
            mariaEventApplyService.applyToEvent(eventApply);
        }

        // When
        final List<EventApply> usersWithPointRewards = mariaEventApplyService.getUsersWithSpecificRewardType("POINT", TEST_EVENT_ID);

        // Then
        assertEquals(10, usersWithPointRewards.size());
    }

    @Test
    @Transactional
    public void test6_GetUsersWithCouponRewards() {
        // Given
        final Reward reward = createAndSaveReward(TEST_EVENT_ID, "COUPON");

        for (long i = 1L; 10L >= i; i++) {
            final EventApplyRequest eventApply = createEventApply(TEST_EVENT_ID, i, reward.getRewardType());
            mariaEventApplyService.applyToEvent(eventApply);
        }

        // When
        final List<EventApply> usersWithCouponRewards = mariaEventApplyService.getUsersWithSpecificRewardType("COUPON", TEST_EVENT_ID);

        // Then
        assertEquals(10, usersWithCouponRewards.size());
    }
}
