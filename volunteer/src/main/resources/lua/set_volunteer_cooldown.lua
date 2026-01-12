-- 批量设置 cooldown
-- KEYS[1] = cooldown key prefix
-- ARGV[1..n-1] = volunteerIds
-- ARGV[n] = ttlSeconds
for i = 1, #ARGV - 1 do
  local key = KEYS[1] .. ARGV[i]
  redis.call('SET', key, '1')
  redis.call('EXPIRE', key, ARGV[#ARGV])
end
return 1
