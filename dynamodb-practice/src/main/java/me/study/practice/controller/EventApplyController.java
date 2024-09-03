package me.study.practice.controller;

import lombok.RequiredArgsConstructor;
import me.study.practice.domain.EventApply;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dynamo/event-apply")
public class EventApplyController {

    @PostMapping("/apply")
    public EventApply apply() {
        return null;
    }

    @GetMapping("/user/{userId}")
    public List<EventApply> getApplyHistory(@PathVariable("userId") Long userId) {
        return null;
    }

    @GetMapping("/rewards/point")
    public List<EventApply> getUsersForPointRewards() {
        return null;
    }

    @GetMapping("/rewards/coupon")
    public List<EventApply> getUsersForCouponRewards() {
        return null;
    }
}

//    @GetMapping("/user/{userId}")
//    public List<EventApply> getUserApplications(@PathVariable Long userId) {
//        return mariaEventApplyService.getUserApplications(userId);
//    }
//
//    @GetMapping("/rewards/point")
//    public List<EventApply> getUsersWithPointRewards() {
//        return mariaEventApplyService.getUsersWithSpecificRewardType("POINT", "SUCCESS");
//    }
//
//    @GetMapping("/rewards/coupon")
//    public List<EventApply> getUsersWithCouponRewards() {
//        return mariaEventApplyService.getUsersWithSpecificRewardType("COUPON", "SUCCESS");