package org.hgc.suts.picture.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {

    /**
     * 域名
     */
    private String host;

    /**
     * secretId
     */
    private String secretId;

    /**
     * 密钥（注意不要泄露）
     */
    private String secretKey;

    /**
     * 区域
     */
    private String region;

    /**
     * 桶名
     */
    private String bucket;

    @Bean
    public COSClient cosClient() {
        // 1. 初始化用户身份信息
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2. 设置 bucket 的地域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        // 3. 生成 cos 客户端。
        return new COSClient(cred, clientConfig);
    }
}
