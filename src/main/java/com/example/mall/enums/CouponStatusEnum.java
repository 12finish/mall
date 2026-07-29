package com.example.mall.enums;

import lombok.Getter;

@Getter
public enum CouponStatusEnum {

    UNUSED(1, "未使用"),
    LOCKED(2, "已锁定"),
    USED(3, "已使用"),
    EXPIRED(4, "已过期"),
    INVALID(5, "已作废");

    private final Integer code;
    private final String desc;

    CouponStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
