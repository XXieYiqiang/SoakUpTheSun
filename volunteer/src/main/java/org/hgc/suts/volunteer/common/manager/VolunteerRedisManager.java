package org.hgc.suts.volunteer.common.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.volunteer.common.constant.RedisCacheConstant;

import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class VolunteerRedisManager {

    private final StringRedisTemplate stringRedisTemplate;

    // 注入配置类里定义的 Lua 脚本 Bean
    private final DefaultRedisScript<List> volunteerActiveMatch;
    private final DefaultRedisScript<Long> addVolunteerToActivePoolScript;
    private final DefaultRedisScript<Long> setCooldownScript;

    // 志愿者入匹配池
    public void addVolunteerToActivePool(Long volunteerId, double lat, double lon, int age, int sex, long ttlSeconds) {
        stringRedisTemplate.execute(
                addVolunteerToActivePoolScript,
                java.util.Arrays.asList(
                        RedisCacheConstant.VOLUNTEER_MATCH_ACTIVE_GEO_KEY,
                        RedisCacheConstant.VOLUNTEER_MATCH_ACTIVE_INFO_PREFIX_KEY
                ),
                String.valueOf(lon),
                String.valueOf(lat),
                volunteerId.toString(),
                String.valueOf(sex),
                String.valueOf(age),
                String.valueOf(ttlSeconds)
        );
    }

    // 志愿者入匹配池
    public void oldAddVolunteerToActivePool(Long volunteerId, double lat, double lon, int age, int sex, long ttlSeconds) {
        // 1. GEO 添加
        stringRedisTemplate.opsForGeo().add(RedisCacheConstant.VOLUNTEER_MATCH_ACTIVE_GEO_KEY,
                new RedisGeoCommands.GeoLocation<>(
                        volunteerId.toString(),
                        new Point(lon, lat)
                )
        );

        // 2. 写入HASH
        String infoKey = RedisCacheConstant.VOLUNTEER_MATCH_ACTIVE_INFO_PREFIX_KEY + volunteerId;
        stringRedisTemplate.opsForHash().put(infoKey, "sex", String.valueOf(sex));
        stringRedisTemplate.opsForHash().put(infoKey, "age", String.valueOf(age));
        stringRedisTemplate.expire(infoKey, ttlSeconds, TimeUnit.SECONDS);
    }


    // 执行lua匹配
    public List<Long> matchBestVolunteers(double userLat, double userLon, int reqAge, int reqSex, int targetCount, double sexWeight, double ageWeight, double locationWeight) {
        // 1. 准备 KEYS
        List<String> keys = new ArrayList<>();
        keys.add(RedisCacheConstant.VOLUNTEER_MATCH_ACTIVE_GEO_KEY);
        keys.add(RedisCacheConstant.VOLUNTEER_MATCH_ACTIVE_INFO_PREFIX_KEY);
        keys.add(RedisCacheConstant.VOLUNTEER_MATCH_COOLDOWN_PREFIX_KEY);

        // 2. 准备 ARGV
        Object[] args = new Object[]{
                String.valueOf(userLat),
                String.valueOf(userLon),
                // 半径
                "1000",
                // 单位
                "km",
                String.valueOf(targetCount),
                String.valueOf(reqAge),
                String.valueOf(reqSex),
                String.valueOf(sexWeight),
                String.valueOf(ageWeight),
                String.valueOf(locationWeight)
        };

        try {
            // 3. 执行脚本
            // lua脚本对于list，只能传入原始的list，所以使用list<?>
            List<?> result = stringRedisTemplate.execute(volunteerActiveMatch, keys, args);
            // 如果返回值是空的
            if (result.isEmpty()) {
                return Collections.emptyList();
            }

            // 转换结果,返回的是Long
            return result.stream()
                    .map(obj -> Long.parseLong(obj.toString()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Lua 匹配脚本执行异常", e);
            return Collections.emptyList();
        }
    }

    public void setCooldown(List<Long> volunteerIds, long seconds) {
        if (volunteerIds == null || volunteerIds.isEmpty()) return;

        java.util.List<String> args = new java.util.ArrayList<>();
        for (Long id : volunteerIds) {
            args.add(id.toString());
        }
        args.add(String.valueOf(seconds));

        stringRedisTemplate.execute(
                setCooldownScript,
                java.util.Collections.singletonList(RedisCacheConstant.VOLUNTEER_MATCH_COOLDOWN_PREFIX_KEY),
                args.toArray()
        );
    }

    // 设置频控
    public void oldSetCooldown(List<Long> volunteerIds, long seconds) {
        if (volunteerIds == null || volunteerIds.isEmpty()) return;
        for (Long id : volunteerIds) {
            String key = RedisCacheConstant.VOLUNTEER_MATCH_COOLDOWN_PREFIX_KEY + id;
            stringRedisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(seconds));
        }
    }
}