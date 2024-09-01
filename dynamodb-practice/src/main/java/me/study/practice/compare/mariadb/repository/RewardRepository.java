package me.study.practice.compare.mariadb.repository;

import jakarta.persistence.LockModeType;
import me.study.practice.compare.mariadb.domain.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RewardRepository extends JpaRepository<Reward, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reward r WHERE r.rewardType = :rewardType AND r.eventId = :eventId")
    Optional<Reward> findByRewardTypeAndEventIdWithLock(@Param("rewardType") String rewardType, @Param("eventId") String eventId);

    List<Reward> findByRewardType(String rewardType);

    Optional<Reward> findByRewardTypeAndEventId(String rewardType, String eventId);
}
