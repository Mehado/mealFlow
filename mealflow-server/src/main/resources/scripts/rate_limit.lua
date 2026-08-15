-- 接口限流脚本：固定窗口计数，Redis 原子执行
-- KEYS[1] = 限流 key（rate:limit:{ip}:{method}）
-- ARGV[1] = 窗口内最大次数
-- ARGV[2] = 窗口秒数
-- 返回值：1=放行，0=超限

local count = redis.call('incr', KEYS[1])
if count == 1 then redis.call('expire', KEYS[1], tonumber(ARGV[2])) end
if count > tonumber(ARGV[1]) then return 0 end
return 1
