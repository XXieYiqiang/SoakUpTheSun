package org.hgc.suts.volunteer.mq.consumer;

import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.mq.base.MessageWrapper;
import org.hgc.suts.volunteer.mq.event.VolunteerUserEsSyncEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 志愿者用户ES同步消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "volunteerTask_excel_es_topic",
        consumerGroup = "volunteerTask_excel_es_cg"
)
public class VolunteerUserEsSyncConsumer implements RocketMQListener<MessageWrapper<VolunteerUserEsSyncEvent>> {

    private final ElasticsearchClient elasticsearchClient;
    private static final String ES_INDEX_NAME = "volunteer_index";

    @Override
    public void onMessage(MessageWrapper<VolunteerUserEsSyncEvent> messageWrapper) {
        // 开头打印日志，平常可 Debug 看任务参数，线上可报平安（比如消息是否消费，重新投递时获取参数等）
        log.info("[消费者] 志愿者推送es任务正式执行 - 执行消费逻辑，消息批次：{}", JSON.toJSONString(messageWrapper.getMessage().getBatchId()));
        // 1. 从消息包装类中解包，获取实际的业务事件对象（包含批次ID和数据）
        VolunteerUserEsSyncEvent event = messageWrapper.getMessage();

        // 2. 从事件中提取出需要同步到 ES 的志愿者数据列表
        List<VolunteerUserDO> userList = event.getUserList();

        try {
            // 3. 创建 BulkRequest 构建器。
            BulkRequest.Builder br = new BulkRequest.Builder();

            // 4. 遍历志愿者列表，为每一条数据构建一个写入操作
            for (VolunteerUserDO userDO : userList) {
                // 5. 向批量构建器中添加操作
                br.operations(entry -> entry
                        // 指定操作类型为 index (索引操作：如果ID不存在则创建，存在则覆盖/更新)
                        .index(idx -> idx
                                .index(ES_INDEX_NAME) // 指定写入的目标索引名称
                                .id(userDO.getId().toString())
                                .document(userDO)
                        )
                );
            }

            // 向 ES 发送批量写入命令，获取返回结果
            BulkResponse result = elasticsearchClient.bulk(br.build());

            // 7. 检查批量操作结果。Bulk 请求整体可能成功，但其中某几条数据可能失败
            if (result.errors()) {
                // 记录错误日志，统计失败的条数，方便排查
                log.error("[消费者] ES批量写入存在失败，批次ID: {}", event.getBatchId());

//                测试使用
                result.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> {
                            // 打印 ID 和 具体的错误原因 (reason)
                            log.error("写入失败 - ID: {}, 原因: {}, 类型: {}",
                                    item.id(),
                                    item.error().reason(),
                                    item.error().type());
                        });

                // RocketMQ 监听器捕获到异常后，会认为消费失败，自动重新投递该消息。
                throw new RuntimeException("ES 批量索引有错误。触发重试.");
            }

        } catch (Exception e) {
            // 8. 捕获执行过程中的任何异常（如网络中断、ES宕机、序列化失败等）并记录日志
            log.error("[消费者] 志愿者推送es任务执行错误，消费批次为：{}", event.getBatchId(), e);

            // 【关键点】再次抛出异常。
            // 必须抛出异常让 RocketMQ 感知到消费失败，否则 RocketMQ 会认为消费成功从而提交 Offset，导致数据丢失。
            throw new RuntimeException("ES同步失败，触发重试.", e);
        }
    }
}