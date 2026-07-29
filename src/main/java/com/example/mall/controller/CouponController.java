package com.example.mall.controller;

import com.example.mall.common.Result;
import com.example.mall.dto.ClaimCouponRequest;
import com.example.mall.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 领取优惠券
     */
    @PostMapping("/claim")
    public Result<String> claim(@Valid @RequestBody ClaimCouponRequest request) {
        String couponNo = couponService.claimCoupon(request.getUserId(), request.getTemplateId());
        return Result.success(couponNo);
    }

    /**
     * 预热优惠券库存到 Redis（运营后台调用）
     */
    @PostMapping("/preload/{templateId}")
    public Result<Void> preload(@PathVariable Long templateId) {
        couponService.preloadRemainCount(templateId);
        return Result.success();
    }
}
