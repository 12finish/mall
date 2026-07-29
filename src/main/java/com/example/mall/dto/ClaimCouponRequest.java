package com.example.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClaimCouponRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;
}
