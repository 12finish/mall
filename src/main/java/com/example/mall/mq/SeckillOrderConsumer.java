package com.example.mall.mq;

import com.example.mall.config.RabbitConfig;
import com.example.mall.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RabbitListener(queues = RabbitConfig.SECKILL_QUEUE)
public class SeckillOrderConsumer {

    private final SeckillService seckillService;

    @RabbitHandler
    public void handle(SeckillService.SeckillOrderMessage message) {
        log.info("收到秒杀订单消息, orderNo={}, userId={}", message.getOrderNo(), message.getUserId());
        try {
            seckillService.createOrder(message);
        } catch (Exception e) {
            log.error("秒杀订单处理失败, orderNo={}", message.getOrderNo(), e);
            // 可接入死信队列或告警
        }
    }
}
