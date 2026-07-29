package com.example.mall.controller;

import com.example.mall.common.Result;
import com.example.mall.dto.SeckillRequest;
import com.example.mall.dto.SeckillTokenRequest;
import com.example.mall.service.SeckillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 请求秒杀资格 token
     */
    @PostMapping("/token")
    public Result<String> token(@Valid @RequestBody SeckillTokenRequest request) {
        String token = seckillService.requestToken(request.getActivityId(), request.getUserId());
        return Result.success(token);
    }

    /**
     * 提交秒杀订单
     */
    @PostMapping("/order")
    public Result<String> order(@Valid @RequestBody SeckillRequest request) {
        String orderNo = seckillService.submitOrder(request.getActivityId(),
                request.getUserId(), request.getQuantity(), request.getToken());
        return Result.success(orderNo);
    }

    /**
     * 预热秒杀库存到 Redis（运营后台调用）
     */
    @PostMapping("/preload/{activityId}")
    public Result<Void> preload(@PathVariable Long activityId) {
        seckillService.preloadStock(activityId);
        return Result.success();
    }
}
