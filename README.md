# 电商小程序后端初版

基于 Spring Boot 3 + MyBatis-Plus + Redis + RabbitMQ 的电商后端初版，包含优惠券领取/锁定/核销、秒杀抢购等高并发场景示例。

## 一键云端运行（推荐）

本项目已配置 GitHub Codespaces，无需本地安装任何环境：

1. 访问 https://github.com/12finish/mall
2. 点击 `Code` → `Codespaces` → `Create codespace on main`
3. 等待 2~3 分钟自动安装 Java、Maven、Docker 并启动服务
4. 服务启动后，在浏览器中访问 `http://localhost:8080`

Codespaces 会自动完成：
- 启动 MySQL、Redis、RabbitMQ
- 执行 `mvn clean compile`
- 运行 `mvn spring-boot:run`

## 技术栈

- Spring Boot 3.2.5
- MyBatis-Plus 3.5.5
- MySQL 8
- Redis 7
- RabbitMQ 3
- Redisson

## 本地快速启动

### 1. 启动基础设施

```bash
docker compose up -d
```

将启动 MySQL（端口 3306）、Redis（端口 6379）、RabbitMQ（端口 5672 / 管理台 15672）。

### 2. 启动应用

```bash
mvn spring-boot:run
```

## 主要接口

### 优惠券

- **领取优惠券**：`POST /api/coupon/claim`
  ```json
  {
    "userId": 1,
    "templateId": 1
  }
  ```
- **预热券库存**：`POST /api/coupon/preload/{templateId}`

### 秒杀

- **请求秒杀 token**：`POST /api/seckill/token`
  ```json
  {
    "activityId": 1,
    "userId": 1
  }
  ```
- **提交秒杀订单**：`POST /api/seckill/order`
  ```json
  {
    "activityId": 1,
    "userId": 1,
    "quantity": 1,
    "token": "上一步返回的 token"
  }
  ```
- **预热秒杀库存**：`POST /api/seckill/preload/{activityId}`

## 并发控制说明

- **优惠券领取**：Redis 原子扣减库存 + Redisson 分布式锁 + DB 乐观锁，防止超发和重复领取。
- **秒杀抢购**：Redis Lua 原子扣减库存 + token 防重 + 分布式锁 + RabbitMQ 异步下单 + DB 唯一索引幂等 + 乐观锁扣减真实库存。

## 测试数据

`sql/init.sql` 已初始化：

- 测试用户 1 人
- 测试商品 SKU 1 个
- 优惠券模板 1 个（满 100 减 20）
- 秒杀活动 1 个（库存 10）
