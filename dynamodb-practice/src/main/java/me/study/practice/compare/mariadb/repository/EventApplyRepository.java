package me.study.practice.compare.mariadb.repository;

import me.study.practice.compare.mariadb.domain.EventApply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventApplyRepository extends JpaRepository<EventApply, String> {
    List<EventApply> findByUserId(Long userId);

    boolean existsByEventIdAndUserId(String eventId, Long userId);

    List<EventApply> findByRewardIdAndEventId(String rewardId, String eventId);

    long countByEventId(String eventId);
}
