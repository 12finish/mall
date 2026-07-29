package com.example.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SeckillTokenRequest {

    @NotNull(message = "活动ID不能为空")
    private Long activityId;

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
