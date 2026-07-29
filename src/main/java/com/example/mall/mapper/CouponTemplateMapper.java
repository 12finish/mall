package com.example.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.entity.CouponTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CouponTemplateMapper extends BaseMapper<CouponTemplate> {

    @Update("UPDATE coupon_template SET remain_count = remain_count - 1, version = version + 1 " +
            "WHERE id = #{id} AND remain_count > 0 AND version = #{version}")
    int decreaseRemain(@Param("id") Long id, @Param("version") Integer version);
}
