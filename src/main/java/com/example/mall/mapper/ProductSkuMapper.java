package com.example.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.mall.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    @Update("UPDATE product_sku SET stock = stock - #{quantity}, version = version + 1 " +
            "WHERE id = #{skuId} AND stock >= #{quantity} AND version = #{version}")
    int decreaseStock(@Param("skuId") Long skuId,
                      @Param("quantity") Integer quantity,
                      @Param("version") Integer version);
}
