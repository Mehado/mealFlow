# MealFlow · 高并发外卖平台

基于 Spring Boot 4 + Redis + RabbitMQ 的外卖点餐系统，覆盖用户端 / 管理端完整业务闭环：下单支付、购物车、菜品套餐、订单履约、报表统计、催单提醒。

## 技术亮点

- **库存防超卖**：Redis Lua 脚本原子扣减，套餐拆解聚合，下单/取消/超时三路径回补，条件更新保证并发幂等
- **缓存一致性**：Cache-Aside + 空值缓存防穿透 + setnx 互斥锁防击穿 + 随机 TTL 防雪崩
- **MQ 可靠性**：生产者 Confirm / Returns 双确认，延迟队列 15 分钟自动关单，失败进重试队列（限 3 次）幂等消费
- **权限矩阵**：`@RequireRole` 注解 + 数据库触发器双层兜底，老板账号唯一保护
- **可观测性**：TraceId 全链路日志（MDC 透传 + 接口耗时统计），按天滚动归档
- **AOP 防护**：接口限流、防重复提交、公共字段自动填充

## 技术栈

Spring Boot 4.0 · MyBatis · MySQL · Redis · RabbitMQ · WebSocket · JWT · Knife4j · 阿里云 OSS · 微信支付

## 模块结构

```
mealflow-common   通用工具、异常、常量、上下文
mealflow-pojo     实体、DTO、VO
mealflow-server   控制器、服务、MQ、任务、切面
```

## 本地启动

1. 执行 `sql/` 目录下脚本初始化数据库（含权限矩阵与库存字段）
2. 配置 `application-dev.yml` 中的 MySQL / Redis / RabbitMQ 连接
3. 启动 `MealflowApplication`，访问 `http://localhost:8080/doc.html` 查看接口文档

## 测试

```bash
mvn -o test
```

覆盖缓存工具、库存扣减、订单提交流程，以及 100 线程并发抢库存的集成测试。
