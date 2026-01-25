-- 原子写入 GEO + HASH + EXPIRE
-- KEYS[1] = GEO key
-- KEYS[2] = info hash prefix
-- ARGV[1] = lon
-- ARGV[2] = lat
-- ARGV[3] = member (volunteerId)
-- ARGV[4] = sex
-- ARGV[5] = age
-- ARGV[6] = ttlSeconds
redis.call('GEOADD', KEYS[1], ARGV[1], ARGV[2], ARGV[3])
local infoKey = KEYS[2] .. ARGV[3]
redis.call('HSET', infoKey, 'sex', ARGV[4], 'age', ARGV[5])
redis.call('EXPIRE', infoKey, ARGV[6])
return 1
