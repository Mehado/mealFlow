-- 分布式锁释放脚本：值匹配才删除，防止 TTL 过期后误删别人的锁
-- KEYS[1] = 锁 key
-- ARGV[1] = 持有锁的线程/请求标识
-- 返回值：1=删除成功，0=值不匹配（锁已不属于自己）

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
