package org.hgc.suts.picture.mq.consumer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.InputStreamResource;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.hgc.suts.picture.common.constant.RedisCacheConstant;
import org.hgc.suts.picture.common.tensentCos.CosManager;
import org.hgc.suts.picture.mq.base.MessageWrapper;
import org.hgc.suts.picture.mq.event.UploadPictureAnalysisEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "picture_analysis_topic",
        consumerGroup = "picture_analysis_cg"
)
@Slf4j
public class PictureAnalysisConsumer implements RocketMQListener<MessageWrapper<UploadPictureAnalysisEvent>> {


    private final CosManager cosManager;
    private final StringRedisTemplate stringRedisTemplate;
    // py端，进行场景识别地址
    private final String aiApiUrl = "http://127.0.0.1:20000/api/vision-qa/qwen";
    @Override
    public void onMessage(MessageWrapper<UploadPictureAnalysisEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 图片分析正式执行 - 执行消费逻辑，消息体：{}", JSON.toJSONString(messageWrapper));

        UploadPictureAnalysisEvent event = messageWrapper.getMessage();

        Long pictureId = event.getPictureId();
        String imageKey= event.getImageKey();

        // url清洗
        try {
            // 1. 如果是完整 URL (http开头)，提取路径部分
            if (StrUtil.startWith(imageKey, "http")) {
                // 解析URL
                imageKey = new java.net.URL(imageKey).getPath();
            }

            // 2. URL 解码
            imageKey = cn.hutool.core.util.URLUtil.decode(imageKey);

            // 3. 去掉开头的斜杠 (/)
            while (StrUtil.startWith(imageKey, "/")) {
                imageKey = imageKey.substring(1);
            }

        } catch (Exception e) {
            log.error("URL 解析失败，将尝试使用原始 Key", e);
        }



        if (ObjectUtil.isEmpty(imageKey) || ObjectUtil.isEmpty(pictureId)) {
            log.info("[消费者] 图片分析正式执行 - 消费失败，消息体不完整，消息体：{}", JSON.toJSONString(event));
        }

        COSObject cosObject;
        COSObjectInputStream cosContent = null;
        try {
            // 1. 下载流
            cosObject = cosManager.getObject(imageKey);
            cosContent = cosObject.getObjectContent();

            // 图片资源
            InputStreamResource pictureResource = new InputStreamResource(cosContent, FileUtil.getName(imageKey));
            // 获取用户描述（可能为空）
            String description = event.getDescriptionContent();
            if (StrUtil.isBlank(description)) {
                description = "请分析这张图片"; // 或给一个默认提示，比如 "请分析这张图片"
            }
            // 2. 转发给 Python
            HttpResponse response = HttpRequest.post(aiApiUrl)
                    .form("file", pictureResource)
                    .form("description", description)
                    .timeout(30000)
                    .execute();

            String aiResultJson = response.body();
            if (!response.isOk()) {
                throw new RuntimeException("AI 接口报错: " + response.getStatus());
            }

            // 3. 存入 Redis
            String pictureAnalysisKey= String.format(RedisCacheConstant.PICTURE_ANALYSIS_RESPONSE_KEY,pictureId);
            stringRedisTemplate.opsForValue().set(pictureAnalysisKey, aiResultJson, 5, TimeUnit.MINUTES);
            log.info("分析完成，结果已存入 Redis");

        } catch (Exception e) {
            log.error("消费失败", e);
            throw new RuntimeException("消费异常，重试", e);
        } finally {
            if (cosContent != null) {
                try {
                    cosContent.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
