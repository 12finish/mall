package com.example.mall.service;

import com.example.mall.entity.CouponTemplate;
import com.example.mall.entity.UserCoupon;
import com.example.mall.enums.CouponStatusEnum;
import com.example.mall.mapper.CouponTemplateMapper;
import com.example.mall.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponTemplateMapper couponTemplateMapper;
    private final UserCouponMapper userCouponMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate;

    private static final String COUPON_REMAIN_KEY = "coupon:remain:";

    /**
     * 领取优惠券：Redis 扣减库存 + Redisson 分布式锁 + DB 乐观锁兜底
     */
    @Transactional(rollbackFor = Exception.class)
    public String claimCoupon(Long userId, Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template == null || template.getStatus() != 1) {
            throw new RuntimeException("优惠券不存在或已下架");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(template.getClaimStartTime()) || now.isAfter(template.getClaimEndTime())) {
            throw new RuntimeException("不在领取时间范围内");
        }

        // 1. Redis 原子扣减库存
        Long remain = redisTemplate.opsForValue().decrement(COUPON_REMAIN_KEY + templateId);
        if (remain == null || remain < 0) {
            // 库存不足，回滚
            redisTemplate.opsForValue().increment(COUPON_REMAIN_KEY + templateId);
            throw new RuntimeException("优惠券已领完");
        }

        // 2. 用户级分布式锁，防止并发重复领取
        String lockKey = "coupon:claim:" + userId + ":" + templateId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean dbSuccess = false;
        try {
            boolean acquired = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("操作太频繁，请稍后再试");
            }

            // 3. 再次校验限领
            long claimedCount = userCouponMapper.countByUserAndTemplate(userId, templateId);
            if (claimedCount >= template.getLimitPerUser()) {
                throw new RuntimeException("已超过领取上限");
            }

            // 4. DB 乐观锁扣减模板剩余量
            int affected = couponTemplateMapper.decreaseRemain(templateId, template.getVersion());
            if (affected == 0) {
                throw new RuntimeException("优惠券已领完");
            }

            // 5. 生成用户券
            UserCoupon userCoupon = new UserCoupon();
            userCoupon.setCouponNo(UUID.randomUUID().toString().replace("-", ""));
            userCoupon.setUserId(userId);
            userCoupon.setTemplateId(templateId);
            userCoupon.setStatus(CouponStatusEnum.UNUSED.getCode());
            userCoupon.setValidStartTime(template.getValidStartTime());
            userCoupon.setValidEndTime(template.getValidEndTime());
            userCoupon.setSource(1);
            userCouponMapper.insert(userCoupon);

            dbSuccess = true;
            return userCoupon.getCouponNo();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
            if (!dbSuccess) {
                // DB 操作失败，回滚 Redis 库存
                redisTemplate.opsForValue().increment(COUPON_REMAIN_KEY + templateId);
            }
        }
    }

    /**
     * 锁定优惠券：下单时使用
     */
    @Transactional(rollbackFor = Exception.class)
    public void lockCoupon(Long userCouponId, String orderNo) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !CouponStatusEnum.UNUSED.getCode().equals(userCoupon.getStatus())) {
            throw new RuntimeException("优惠券不可用");
        }
        int affected = userCouponMapper.updateStatus(userCouponId,
                CouponStatusEnum.LOCKED.getCode(),
                CouponStatusEnum.UNUSED.getCode(),
                orderNo,
                userCoupon.getVersion());
        if (affected == 0) {
            throw new RuntimeException("优惠券锁定失败，可能已被使用");
        }
    }

    /**
     * 核销优惠券：支付成功后调用
     */
    @Transactional(rollbackFor = Exception.class)
    public void useCoupon(Long userCouponId, String orderNo) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !CouponStatusEnum.LOCKED.getCode().equals(userCoupon.getStatus())) {
            throw new RuntimeException("优惠券状态异常");
        }
        int affected = userCouponMapper.updateStatus(userCouponId,
                CouponStatusEnum.USED.getCode(),
                CouponStatusEnum.LOCKED.getCode(),
                orderNo,
                userCoupon.getVersion());
        if (affected == 0) {
            throw new RuntimeException("优惠券核销失败");
        }
    }

    /**
     * 释放优惠券：订单取消或支付超时调用
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseCoupon(Long userCouponId) {
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !CouponStatusEnum.LOCKED.getCode().equals(userCoupon.getStatus())) {
            return;
        }
        userCouponMapper.updateStatus(userCouponId,
                CouponStatusEnum.UNUSED.getCode(),
                CouponStatusEnum.LOCKED.getCode(),
                null,
                userCoupon.getVersion());
    }

    /**
     * 运营后台创建模板后，预热 Redis 库存
     */
    public void preloadRemainCount(Long templateId) {
        CouponTemplate template = couponTemplateMapper.selectById(templateId);
        if (template != null) {
            redisTemplate.opsForValue().set(COUPON_REMAIN_KEY + templateId,
                    String.valueOf(template.getRemainCount()));
        }
    }
}
