package me.study.practice.compare.mariadb.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.study.practice.compare.mariadb.controller.EventApplyRequest;
import me.study.practice.compare.mariadb.domain.EventApply;
import me.study.practice.compare.mariadb.domain.Reward;
import me.study.practice.compare.mariadb.repository.EventApplyRepository;
import me.study.practice.compare.mariadb.repository.RewardRepository;
import me.study.practice.compare.mariadb.repository.RewardRepositoryCustom;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MariaEventApplyService {
    private final EventApplyRepository eventApplyRepository;
    private final RewardRepository rewardRepository;
    private final RewardRepositoryCustom rewardRepositoryCustom;

    @Transactional
    public EventApply applyToEvent(EventApplyRequest eventApplyRequest) {
        if (eventApplyRepository.existsByEventIdAndUserId(eventApplyRequest.getEventId(), eventApplyRequest.getUserId())) {
            throw new RuntimeException("User has already applied to this event.");
        }

        // Reward 조회 및 재고 확인
        Reward reward = rewardRepository.findByRewardTypeAndEventIdWithLock(eventApplyRequest.getRewardType(),
                                                                            eventApplyRequest.getEventId())
                                        .orElseThrow(() -> new RuntimeException("Reward not found."));
        System.out.println(">>>> reward : " + reward.getStockCount() + " >>>> eventRequest.userId : " + eventApplyRequest.getUserId());

        if (reward.getStockCount() <= 0) {
            throw new RuntimeException("No stock available for this reward.");
        }

        // 재고 차감
        reward.setStockCount(reward.getStockCount() - 1);
        rewardRepository.save(reward);

        // 첫 응모 시 applyStatus를 REWARD_PROVIDED로 설정
        EventApply eventApply = new EventApply();
        eventApply.setId("event-apply-" + eventApplyRequest.getEventId() + "-" + eventApplyRequest.getUserId());
        eventApply.setEventId(eventApplyRequest.getEventId());
        eventApply.setUserId(eventApplyRequest.getUserId());
        eventApply.setRewardId(reward.getId());
        eventApply.setApplyStatus("REWARD_PROVIDED");
        System.out.println(">>>>save : " + eventApply);

        return eventApplyRepository.save(eventApply);
    }

    @Transactional
    public List<EventApply> getUserApplications(final Long userId) {
        return eventApplyRepository.findByUserId(userId);
    }

    @Transactional
    public List<EventApply> getUsersWithSpecificRewardType(final String rewardType, final String eventId) {
        return rewardRepository.findByRewardTypeAndEventId(rewardType, eventId)
                               .map(reward -> eventApplyRepository.findByRewardIdAndEventId(reward.getId(), eventId))
                               .orElseThrow(() -> new RuntimeException("Reward not found."));
    }
}
