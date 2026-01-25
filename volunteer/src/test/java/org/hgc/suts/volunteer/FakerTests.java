package org.hgc.suts.volunteer;

import com.github.javafaker.Faker;
import com.github.javafaker.PhoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import java.util.Locale;

/**
 * Faker 单元测试类
 */
@DisplayName("Faker 数据生成测试")
public class FakerTests {

    @Test
    public void testFaker() {
        // 创建一个 Faker 实例
        Faker faker = new Faker(Locale.CHINA);

        // 生成中文名
        String chineseName = faker.name().fullName();
        System.out.println("中文名: " + chineseName);

        // 生成手机号
        PhoneNumber phoneNumber = faker.phoneNumber();
        String mobileNumber = phoneNumber.cellPhone();

        // 生成电子邮箱
        String email = faker.internet().emailAddress();
        System.out.println("电子邮箱: " + email);
    }
}