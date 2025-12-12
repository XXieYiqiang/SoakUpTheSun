package org.hgc.suts.shortlink.utils;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.shortlink.common.exception.ClientException;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 短链接生成工具类
 */
@Component
@RequiredArgsConstructor
public class RandomUtils {

    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    /**
     * 字符集，已去除难分辨字母
     */
    private static final String ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";


    /**
     * 生成短链接码
     * @param length 长度
     * @return 如: x7Km9A
     */
    public String generateShortCode(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("生成短链码错误");
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        StringBuilder sb;
        String code;

        int retryCount = 0;
        int maxRetries = 10;

        do {
            if (retryCount >= maxRetries) {
                // 布隆过滤器碰撞多次，说明需要换数据库的表和布隆过滤器
                throw new RuntimeException("短链生成重试次数过多，请检查布隆过滤器和数据库");
            }

            sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = random.nextInt(ALPHABET.length());
                sb.append(ALPHABET.charAt(index));
            }
            code = sb.toString();

            retryCount++;

        } while (shortUriCreateCachePenetrationBloomFilter.contains(createShortLinkDefaultDomain + "/" +code));

        return code;
    }

    /**
     * 生成验证码/邀请码 (纯数字)
     * @param length 长度
     * @return 如: 8ASD76
     */
    public String generateInviteCode(int length) {
        if (length < 1) {
            throw new ClientException("生成验证码错误");
        }
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }
}