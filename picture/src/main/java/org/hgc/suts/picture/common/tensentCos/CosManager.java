package org.hgc.suts.picture.common.tensentCos;


import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;

import lombok.RequiredArgsConstructor;
import org.hgc.suts.picture.config.CosClientConfig;
import org.springframework.stereotype.Component;


import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CosManager {

    private final CosClientConfig cosClientConfig;

    private final COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     * @param key           唯一键
     * @param inputStream   文件输入流
     * @param contentLength 文件流大小（必须传，否则 SDK 可能会报错或内存溢出）
     */
    public PutObjectResult putPictureObject(String key, InputStream inputStream, long contentLength) {
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(contentLength);

        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                inputStream, objectMetadata);

        // 对图片进行处理（获取基本信息也被视作为一种图片的处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);

        // 图片处理规则列表
        List<PicOperations.Rule> rules = new ArrayList<>();

        // 1. 图片压缩（转成 webp 格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);

        // 2. 缩略图处理，仅对 > 2KB 的图片生成缩略图
        if (contentLength > 2 * 1024) {
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            // 拼接缩略图的路径
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
            thumbnailRule.setFileId(thumbnailKey);
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            // 缩放规则 /thumbnail/<Width>x<Height>>（如果大于原图宽高，则不处理）
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
            rules.add(thumbnailRule);
        }

        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);

        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}