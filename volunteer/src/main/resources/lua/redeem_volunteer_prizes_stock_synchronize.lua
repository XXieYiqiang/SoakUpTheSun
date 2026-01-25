-- Lua 脚本: 领取逻辑
-- 返回值：
-- 0: 成功
-- 1: 库存不足
-- 2: 已经领取过了

-- 获取当前库存
local stock = tonumber(redis.call('HGET', KEYS[1], 'stock'))

-- 1. 校验库存
if stock <= 0 then
    -- 失败,库存不足
    return 1
end

-- 2. 校验用户是否已领取
if redis.call('EXISTS', KEYS[2]) == 1 then
    -- 失败,已经领取过了
    return 2
end

-- 3. 执行领取
-- 记录用户领取记录
redis.call('SET', KEYS[2], 1)
redis.call('EXPIRE', KEYS[2], ARGV[1])

-- 扣减库存
redis.call('HINCRBY', KEYS[1], 'stock', -1)

-- 4. 返回成功
return 0
