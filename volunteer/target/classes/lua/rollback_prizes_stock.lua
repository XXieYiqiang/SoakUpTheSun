-- KEYS[1]: 库存 Key
-- KEYS[2]: 用户抢购记录 Key

-- 1. 库存加回 1
redis.call('incr', KEYS[1])
-- 2. 删除用户的抢购记录
redis.call('del', KEYS[2])
return 1