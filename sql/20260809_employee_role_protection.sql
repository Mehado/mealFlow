-- ============================================================
-- 老板账号保护（数据库层兜底）
-- 1. 禁止删除 role='OWNER' 的员工
-- 2. 禁止禁用 role='OWNER' 的员工（status -> 0）
-- 防止误操作导致项目失去可登录的管理账号
-- ============================================================
USE sky_take_out;
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS trg_employee_block_delete_owner;
DELIMITER $$
CREATE TRIGGER trg_employee_block_delete_owner
BEFORE DELETE ON employee
FOR EACH ROW
BEGIN
    IF OLD.role = 'OWNER' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '老板账号禁止删除，请保留至少一个老板账号';
    END IF;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trg_employee_block_disable_owner;
DELIMITER $$
CREATE TRIGGER trg_employee_block_disable_owner
BEFORE UPDATE ON employee
FOR EACH ROW
BEGIN
    IF OLD.role = 'OWNER' AND NEW.status = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '老板账号禁止禁用，请保留至少一个老板账号';
    END IF;
END$$
DELIMITER ;

-- 校验：查看已创建的触发器
SHOW TRIGGERS LIKE 'employee';
