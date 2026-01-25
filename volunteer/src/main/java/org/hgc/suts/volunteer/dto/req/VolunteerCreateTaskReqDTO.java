package org.hgc.suts.volunteer.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 添加志愿者请求实体
 */
@Data
public class VolunteerCreateTaskReqDTO {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * Excel 文件地址
     */
    private String fileAddress;

    /**
     * 发送类型 0：立即发送 1：定时发送
     */
    private Integer sendType;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;
}
