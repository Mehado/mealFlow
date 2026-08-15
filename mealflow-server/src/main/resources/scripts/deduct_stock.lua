-- 库存预扣脚本：检查 + 扣减 原子执行
-- KEYS[1] = 库存 key（如 stock:dish:1）
-- ARGV[1] = 本次要扣减的数量
-- 返回值：1=成功，-1=库存不足，-2=库存未预热

local stock = tonumber(redis.call('get', KEYS[1]) or '-1')
if stock == -1 then return -2 end
if stock < tonumber(ARGV[1]) then return -1 end
redis.call('decrby', KEYS[1], ARGV[1])
return 1
