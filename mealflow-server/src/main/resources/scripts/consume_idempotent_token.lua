-- 下单幂等令牌消费脚本：检查 + 删除 原子执行
-- KEYS[1] = token key（order:submit:token:{token}）
-- ARGV[1] = 当前用户 id
-- 返回值：1=消费成功（token 存在且属于该用户），0=重复提交

if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
