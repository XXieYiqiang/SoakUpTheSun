local geoKey = KEYS[1]
local infoKeyPrefix = KEYS[2]
local cooldownKeyPrefix = KEYS[3]

local userLat = tonumber(ARGV[1])
local userLon = tonumber(ARGV[2])
local radius = ARGV[3]
local radiusUnit = ARGV[4]
local targetCount = tonumber(ARGV[5])

local reqAge = tonumber(ARGV[6])
local reqSex = tonumber(ARGV[7])
local sexWeight = tonumber(ARGV[8])
local ageWeight = tonumber(ARGV[9])
local locationWeight = tonumber(ARGV[10])

-- 1.根据距离筛选50人
local geoResults = redis.call('GEORADIUS', geoKey, userLon, userLat, radius, radiusUnit, 'WITHDIST', 'COUNT', 50, 'ASC')

local candidates = {}

-- 2. 精确匹配
for _, member in ipairs(geoResults) do
    local volunteerId = member[1]
    -- 距离
    local dist = tonumber(member[2])

    -- 检查频控 (Cooldown)
    local isCooldown = redis.call('EXISTS', cooldownKeyPrefix .. volunteerId)

    if isCooldown == 0 then
        -- 获取信息
        local infoKey = infoKeyPrefix .. volunteerId

        -- 先判断 hash 是否存在，不存在则清理 GEO
        local infoExists = redis.call('EXISTS', infoKey)
        -- 清理僵尸 GEO 成员
        if infoExists == 0 then
            redis.call('ZREM', geoKey, volunteerId)
        else
            local info = redis.call('HMGET', infoKey, 'sex', 'age') -- 🟢[新增]
            -- 活跃性检查,存在即活跃
            if info[1] and info[2] then
                local vSex = tonumber(info[1])
                local vAge = tonumber(info[2])

                -- 计算偏差值
                local locDiff = dist * 1000

                local sexDiff = (reqSex - vSex) ^ 2
                local ageDiff = (reqAge - vAge) ^ 2

                -- 计算惩罚值
                local fx = (sexWeight * sexDiff) + (ageWeight * ageDiff) + (locationWeight * locDiff)

                -- 计算最终得分
                local K_SCALE = 0.05
                local score = math.exp(-K_SCALE * fx)

                table.insert(candidates, {volunteerId, score})
            else
                redis.call('ZREM', geoKey, volunteerId)
            end
        end
    end
end

-- 3. 排序，从大到小
table.sort(candidates, function(a, b) return a[2] > b[2] end)

-- 4. 返回top
local resultIds = {}
for i = 1, math.min(#candidates, targetCount) do
    table.insert(resultIds, candidates[i][1])
end

return resultIds
