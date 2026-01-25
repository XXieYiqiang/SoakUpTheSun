package org.hgc.suts.picture.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 实时视频流事件 (传输二进制数据)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFrameAnalysisEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 视障用户ID
     */
    private String userId;

    /**
     * 图片原始二进制数据
     */
    private byte[] imageData;

    /**
     * 时间戳 用于丢弃过期帧
     */
    private Long timestamp;
}