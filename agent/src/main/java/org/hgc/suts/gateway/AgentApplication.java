package org.hgc.suts.gateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * SUTS AI Gateway 启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("org.hgc.suts.gateway.dao.mapper")
@EnableFeignClients(basePackages = "org.hgc.suts.gateway.remote")
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}