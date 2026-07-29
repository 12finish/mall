package com.example.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("coupon_template")
public class CouponTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateCode;
    private String name;
    private Integer type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscountAmount;
    private Integer totalCount;
    private Integer remainCount;
    private Integer limitPerUser;
    private LocalDateTime claimStartTime;
    private LocalDateTime claimEndTime;
    private Integer validDays;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private Integer scopeType;
    private String scopeIds;
    private Integer canStack;
    private Integer channel;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
}
