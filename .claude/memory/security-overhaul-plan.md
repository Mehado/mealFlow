# 苍穹外卖 — 安全改造方案

> 结论日期: 2026-08-06
> 状态: **已完成**（2026-08-07 角色权限体系 + 输入校验均已落地并通过测试）

## 一、已完成的改动

**BCrypt 替换 MD5**（本会话中用户已完成）：
- 加 `spring-security-crypto` 依赖
- 新建 `PasswordEncoderConfiguration.java`
- `EmployeeServiceImpl`: login() 用 `passwordEncoder.matches()`，save() 用 `passwordEncoder.encode()`
- 数据库管理员密码已替换为 BCrypt 值

---

## 二、待执行：JWT 密钥 + 角色权限体系

### 问题 1: JWT 密钥硬编码 + admin/user 同密钥

**改动：**
- `application.yml`: 两个 `*-secret-key` 改为占位符 `${sky.jwt.admin-secret-key}` / `${sky.jwt.user-secret-key}`
- `application-dev.yml`: 用 Jasypt 加密两个不同的 256-bit 随机密钥
- `.gitignore`: 加 `application-dev.yml`

### 问题 2: 没有角色权限控制

**方案：固定 4 角色 + @RequireRole 注解 + AOP 切面**

## 三、4 个角色定义

| 角色 | 常量值 | 职能 |
|------|--------|------|
| 店主 | `OWNER` | 全部权限：菜单/订单/报表/员工管理/开关店 |
| 前台 | `CASHIER` | 订单操作：接单/拒单/退款 |
| 厨师 | `CHEF` | 查看订单 + 标记出餐 |
| 配送员 | `RIDER` | 看待配送 + 标记完成 |

## 四、权限矩阵

| Controller / 接口 | OWNER | CASHIER | CHEF | RIDER |
|---|---|---|---|---|
| **EmployeeController** |||||
| login / logout / editPassword | ✅ | ✅ | ✅ | ✅ |
| save / page / getById / update / startOrStop | ✅ | ❌ | ❌ | ❌ |
| **DishController** — 全部 | ✅ | ❌ | ❌ | ❌ |
| **SetmealController** — 全部 | ✅ | ❌ | ❌ | ❌ |
| **CategoryController** — 全部 | ✅ | ❌ | ❌ | ❌ |
| **ShopController** | ✅ | ❌ | ❌ | ❌ |
| **ReportController** — 全部 | ✅ | ❌ | ❌ | ❌ |
| **WorkSpaceController** — 全部 | ✅ | ✅ | ❌ | ❌ |
| **CommonController** — upload | ✅ | ❌ | ❌ | ❌ |
| **OrderController** |||||
| conditionSearch / statistics | ✅ | ✅ | ✅ | ✅ |
| details | ✅ | ✅ | ✅ | ✅ |
| confirm (接单) | ✅ | ✅ | ❌ | ❌ |
| rejection (拒单) | ✅ | ✅ | ❌ | ❌ |
| cancel (取消) | ✅ | ✅ | ❌ | ❌ |
| delivery (派送) | ✅ | ❌ | ❌ | ❌ |
| complete (完成) | ✅ | ❌ | ❌ | ✅ |

> 注意：editPassword 不需 `@RequireRole`，因为你改别人密码会被"旧密码校验"挡住。

## 五、改动文件清单（共 11 个）

### 数据库

```sql
ALTER TABLE employee ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'STAFF' AFTER status;
UPDATE employee SET role = 'OWNER' WHERE username = 'admin';
```

### 实体 & DTO

1. **`sky-pojo/.../entity/Employee.java`** — 加 `private String role;`
2. **`sky-pojo/.../dto/EmployeeDTO.java`** — 加 `private String role;`（新增员工时可以指定角色）
3. **`sky-server/.../mapper/EmployeeMapper.xml`** — insert 语句加 `role` 列

### Context & 认证链路

4. **`sky-common/.../context/BaseContext.java`** — 加 role 的 ThreadLocal（set/get/remove）
5. **`sky-server/.../controller/admin/EmployeeController.java`** — login() 的 JWT claims 加 `"role"`
6. **`sky-server/.../interceptor/JwtTokenAdminInterceptor.java`** — 解析 JWT 的 role → 存入 BaseContext；afterCompletion 清理

### 注解 & 切面（新建）

7. **`sky-server/.../annotations/RequireRole.java`** — 新建注解
8. **`sky-server/.../aspect/RoleAspect.java`** — 新建切面

### Controller 加注解

9. **EmployeeController** — save/page/getById/update/startOrStop 加 `@RequireRole`
10. **DishController** — 全部接口加 `@RequireRole`
11. **SetmealController** — 全部接口加 `@RequireRole`
12. **CategoryController** — 全部接口加 `@RequireRole`
13. **ShopController** — setStatus 加 `@RequireRole`
14. **ReportController** — 全部接口加 `@RequireRole`
15. **WorkSpaceController** — 全部接口加 `@RequireRole(OWNER)`（或 OWNER+CASHIER）
16. **CommonController** — upload 加 `@RequireRole`
17. **OrderController** — 按权限矩阵各接口加对应角色

### JWT 密钥外部化

18. **application.yml** — jwt secret-key 改为占位符
19. **application-dev.yml** — 用 Jasypt `ENC(...)` 存两个不同密钥
20. **.gitignore** — 加 `application-dev.yml`

## 六、@RequireRole 注解设计

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value() default { "OWNER" };  // 默认仅店主
}
```

默认值 `"OWNER"`，所以大部分接口只需写 `@RequireRole`（不加参数）。
多角色接口写 `@RequireRole({"OWNER", "CASHIER"})`。

## 七、后续扩展伏笔

- Employee 保留完整 CRUD（被 `@RequireRole` 保护），店主可通过后台 UI 赋权
- 将来多商家：各业务表加 `merchant_id`，登录时塞入 JWT + BaseContext，查询自动过滤
- 角色不硬编码在 Controller 里——一处注解 + 切面全部兜住

---

## 相关 memory

- [[dish-mapper-xml-500-error]]
