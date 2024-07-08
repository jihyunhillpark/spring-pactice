package me.study.practice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RequestMapping("/event")
public class EventApplyController {

    @PostMapping("/apply")
    public String apply() {
        return "event/apply";
    }

    @GetMapping("/apply/history/{memberNumber}")
    public String getApplyHistory(@PathVariable("memberNumber") String memberNumber) {
        return "event/apply";
    }

    @GetMapping("/apply/history/point")
    public List<String> getUsersForPointReward() {
        return null;
    }

    @GetMapping("/apply/history/coupon")
    public List<String> getUsersForCouponReward() {
        return null;
    }
}
