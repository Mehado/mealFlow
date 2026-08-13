-- ============================================================
-- 老板账号全局唯一（数据库层兜底）
-- 利用生成列 + 唯一索引：role='OWNER' 时生成 'OWNER'，
-- 否则生成 NULL（NULL 不参与唯一约束），从而保证最多一个老板。
-- 注意：若已执行过本脚本，重复执行 ALTER 会报错，可忽略。
-- ============================================================
USE mealflow;
SET NAMES utf8mb4;

ALTER TABLE employee
    ADD COLUMN role_owner_key VARCHAR(10)
        GENERATED ALWAYS AS (IF(role = 'OWNER', 'OWNER', NULL)) STORED,
    ADD UNIQUE INDEX uk_employee_single_owner (role_owner_key);

-- 校验
SHOW INDEX FROM employee WHERE Key_name = 'uk_employee_single_owner';
