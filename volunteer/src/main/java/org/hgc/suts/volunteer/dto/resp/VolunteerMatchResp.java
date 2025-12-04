package org.hgc.suts.volunteer.dto.resp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 志愿者返回参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerMatchResp {

    /**
     * 志愿者id
     */
    private Long id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别 1/0 男/女
     */
    private Integer sex;

    /**
     * 志愿者手机号
     */
    private String phone;

    /**
     * 生日
     */
    private LocalDateTime birthday;

    /**
     * 经纬度
     */
    private String location;

    /**
     * 评分
     */
    private Double score;

    /**
     * 志愿开始时间
     */
    private String startTime;

    /**
     * 志愿结束时间
     */
    private String endTime;


    /**
     * 匹配度
     */
    private Double matchDegree;
}
