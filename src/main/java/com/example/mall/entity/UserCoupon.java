package com.example.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String couponNo;
    private Long userId;
    private Long templateId;
    private Integer status;
    private String orderNo;
    private LocalDateTime claimTime;
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    private LocalDateTime useTime;
    private Integer source;
    private Integer version;
}
