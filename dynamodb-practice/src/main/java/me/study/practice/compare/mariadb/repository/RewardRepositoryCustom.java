package me.study.practice.compare.mariadb.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import me.study.practice.compare.mariadb.domain.Reward;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RewardRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Reward findByRewardTypeAndEventIdWithLock(String rewardType, String eventId) {
        String sql = "select * from reward where reward_type = :rewardType and event_id = :eventId for update";
        try {
            return (Reward) entityManager.createNativeQuery(sql, Reward.class)
                                         .setParameter("rewardType", rewardType)
                                         .setParameter("eventId", eventId)
                                         .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
