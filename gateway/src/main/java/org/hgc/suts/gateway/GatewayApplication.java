package org.hgc.suts.gateway;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * SUTS AI Gateway 启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("org.hgc.suts.gateway.dao.mapper") // 预埋 Mapper 扫描路径
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}