package org.hgc.suts.volunteer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Configuration
public class RedisScriptConfig {

    /**
     * 新增PrizesCache脚本
     */
    @Bean
    public DefaultRedisScript<Long> savePrizesCacheScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/create_prizes_cache.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 新增领取奖品同步库存脚本
     */
    @Bean
    public DefaultRedisScript<Long> redeemVolunteerPrizesStockSynchronize() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/redeem_volunteer_prizes_stock_synchronize.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 回滚奖品库存
     */
    @Bean
    public DefaultRedisScript<Long> rollbackPrizesStock() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rollback_prizes_stock.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 匹配脚本
     */
    @Bean
    public DefaultRedisScript<List> volunteerActiveMatch() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/volunteer_active_match.lua")));
        script.setResultType(List.class); // 关键修改：Long -> List
        return script;
    }
}