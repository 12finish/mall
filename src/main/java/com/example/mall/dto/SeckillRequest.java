package com.example.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "数量不能为空")
    private Integer quantity;

    @NotNull(message = "秒杀令牌不能为空")
    private String token;
}
