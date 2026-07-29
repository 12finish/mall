package com.example.mall.enums;

import lombok.Getter;

@Getter
public enum CouponTypeEnum {

    AMOUNT(1, "满减券"),
    DISCOUNT(2, "折扣券"),
    NO_THRESHOLD(3, "无门槛券"),
    FREIGHT(4, "运费券");

    private final Integer code;
    private final String desc;

    CouponTypeEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
