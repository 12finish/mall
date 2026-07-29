package com.example.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {

    @Select("SELECT COUNT(*) FROM user_coupon WHERE user_id = #{userId} AND template_id = #{templateId}")
    long countByUserAndTemplate(@Param("userId") Long userId, @Param("templateId") Long templateId);

    @Update("UPDATE user_coupon SET status = #{status}, order_no = #{orderNo}, use_time = NOW(), version = version + 1 " +
            "WHERE id = #{id} AND status = #{expectedStatus} AND version = #{version}")
    int updateStatus(@Param("id") Long id,
                     @Param("status") Integer status,
                     @Param("expectedStatus") Integer expectedStatus,
                     @Param("orderNo") String orderNo,
                     @Param("version") Integer version);
}
