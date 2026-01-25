-- 参数说明：
-- KEYS[1]: 缓存的 Key
-- ARGV[1]...ARGV[N-1]: Hash 的 key-value 对 (通过 unpack 解包)
-- ARGV[N]: 过期时间 (Unix 时间戳，秒)

-- 1. 批量设置 Hash
redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1))

-- 2. 设置过期时间
redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])

return 1