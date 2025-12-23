package org.hgc.suts.volunteer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@MapperScan("org.hgc.suts.volunteer.dao.mapper")
@EnableFeignClients("org.hgc.suts.volunteer.remote")
public class VolunteerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolunteerApplication.class, args);
    }

}
