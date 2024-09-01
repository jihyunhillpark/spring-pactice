package me.study.practice.compare.mariadb.controller;

import lombok.RequiredArgsConstructor;
import me.study.practice.compare.mariadb.domain.EventApply;
import me.study.practice.compare.mariadb.service.MariaEventApplyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/event-apply")
@RequiredArgsConstructor
public class MariaEventApplyController {

    private final MariaEventApplyService mariaEventApplyService;

    @PostMapping("/apply")
    public EventApply applyToEvent(@RequestBody EventApplyRequest eventApplyRequest) {
        return mariaEventApplyService.applyToEvent(eventApplyRequest);
    }

    @GetMapping("/user/{userId}")
    public List<EventApply> getUserApplications(@PathVariable Long userId) {
        return mariaEventApplyService.getUserApplications(userId);
    }

    @GetMapping("/rewards/point")
    public List<EventApply> getUsersWithPointRewards() {
        return mariaEventApplyService.getUsersWithSpecificRewardType("POINT", "SUCCESS");
    }

    @GetMapping("/rewards/coupon")
    public List<EventApply> getUsersWithCouponRewards() {
        return mariaEventApplyService.getUsersWithSpecificRewardType("COUPON", "SUCCESS");
    }
}
