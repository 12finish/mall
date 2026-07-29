package com.example.mall.service;

import com.example.mall.entity.ProductSku;
import com.example.mall.entity.SeckillActivity;
import com.example.mall.entity.SeckillOrder;
import com.example.mall.mapper.ProductSkuMapper;
import com.example.mall.mapper.SeckillActivityMapper;
import com.example.mall.mapper.SeckillOrderMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final ProductSkuMapper productSkuMapper;
    private final StringRedisTemplate redisTemplate;
    private final RedissonClient redissonClient;
    private final RabbitTemplate rabbitTemplate;

    private static final String SECKILL_STOCK_KEY = "seckill:stock:";
    private static final String SECKILL_TOKEN_KEY = "seckill:token:";

    private static final String SECKILL_DEDUCT_LUA =
            "local stock = tonumber(redis.call('get', KEYS[1]))\n" +
            "if stock == nil then return -1 end\n" +
            "if stock < tonumber(ARGV[1]) then return -2 end\n" +
            "return redis.call('decrby', KEYS[1], ARGV[1])";

    /**
     * 秒杀开始前预热库存到 Redis
     */
    public void preloadStock(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity != null) {
            redisTemplate.opsForValue().set(SECKILL_STOCK_KEY + activityId,
                    String.valueOf(activity.getTotalStock()));
        }
    }

    /**
     * 请求秒杀资格，发放 token
     */
    public String requestToken(Long activityId, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            throw new RuntimeException("活动不存在");
        }
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            throw new RuntimeException("活动未开始或已结束");
        }

        // 一人一票，防止重复参与
        String token = UUID.randomUUID().toString().replace("-", "");
        String key = SECKILL_TOKEN_KEY + activityId + ":" + userId;
        Long added = redisTemplate.opsForSet().add(key, token);
        if (added == null || added == 0) {
            throw new RuntimeException("已参与过该活动");
        }
        redisTemplate.expire(key, 5, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 提交秒杀请求：Redis Lua 扣库存 + 分布式锁 + 发送 MQ
     */
    public String submitOrder(Long activityId, Long userId, Integer quantity, String token) {
        // 1. 校验 token
        String tokenKey = SECKILL_TOKEN_KEY + activityId + ":" + userId;
        Boolean hasToken = redisTemplate.opsForSet().isMember(tokenKey, token);
        if (!Boolean.TRUE.equals(hasToken)) {
            throw new RuntimeException("非法请求或已参与");
        }

        // 2. Redis Lua 原子扣减库存
        String stockKey = SECKILL_STOCK_KEY + activityId;
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SECKILL_DEDUCT_LUA);
        script.setResultType(Long.class);
        Long result = redisTemplate.execute(script,
                Collections.singletonList(stockKey), String.valueOf(quantity));
        if (result == null || result < 0) {
            throw new RuntimeException("库存不足");
        }

        // 3. 分布式锁锁定用户 + 活动，防止并发重复下单
        String lockKey = "seckill:order:" + activityId + ":" + userId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(2, 3, TimeUnit.SECONDS);
            if (!acquired) {
                throw new RuntimeException("操作太频繁");
            }

            // 4. 构造 MQ 消息
            SeckillOrderMessage message = new SeckillOrderMessage();
            message.setActivityId(activityId);
            message.setUserId(userId);
            message.setQuantity(quantity);
            message.setOrderNo("SK" + System.currentTimeMillis() + userId);

            rabbitTemplate.convertAndSend("seckill.exchange", "seckill.order", message);
            return message.getOrderNo();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * MQ 消费者实际创建订单
     */
    @Transactional(rollbackFor = Exception.class)
    public void createOrder(SeckillOrderMessage message) {
        SeckillActivity activity = seckillActivityMapper.selectById(message.getActivityId());
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 幂等：数据库唯一索引 activity_id + user_id 防重
        SeckillOrder order = new SeckillOrder();
        order.setActivityId(message.getActivityId());
        order.setOrderNo(message.getOrderNo());
        order.setUserId(message.getUserId());
        order.setSkuId(activity.getSkuId());
        order.setQuantity(message.getQuantity());
        order.setStatus(1);

        try {
            seckillOrderMapper.insert(order);
        } catch (Exception e) {
            log.warn("重复秒杀订单, activityId={}, userId={}", message.getActivityId(), message.getUserId());
            // 可在此处回滚 Redis 库存
            redisTemplate.opsForValue().increment(SECKILL_STOCK_KEY + message.getActivityId(), message.getQuantity());
            return;
        }

        // DB 乐观锁扣减真实库存
        ProductSku sku = productSkuMapper.selectById(activity.getSkuId());
        int affected = productSkuMapper.decreaseStock(activity.getSkuId(), message.getQuantity(), sku.getVersion());
        if (affected == 0) {
            throw new RuntimeException("库存扣减失败");
        }
    }

    /**
     * 内部 MQ 消息对象
     */
    @Data
    public static class SeckillOrderMessage implements Serializable {
        private Long activityId;
        private Long userId;
        private Integer quantity;
        private String orderNo;
    }
}
