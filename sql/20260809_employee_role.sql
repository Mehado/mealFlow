-- ============================================================
-- 权限矩阵数据库迁移：employee 表补充 role 角色列
-- 说明：MySQL 的 ADD COLUMN 不支持 IF NOT EXISTS，
--       若列已存在会报错，可忽略该错误直接执行下方 UPDATE。
-- ============================================================
USE mealflow;

-- 1. employee 表增加角色列（默认 STAFF，最小权限）
ALTER TABLE employee
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'STAFF'
    COMMENT '角色:OWNER老板/CASHIER前台/CHEF厨师/RIDER骑手/STAFF普通员工'
    AFTER status;

-- 2. 把管理员账号设为老板角色（按实际管理员用户名调整）
UPDATE employee
SET role = 'OWNER'
WHERE username = 'admin'
  AND (role IS NULL OR role = '' OR role = 'STAFF');

-- 3. 校验
SELECT id, username, name, role, status FROM employee;
