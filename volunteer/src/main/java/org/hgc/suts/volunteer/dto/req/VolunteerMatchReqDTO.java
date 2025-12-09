package org.hgc.suts.volunteer.dto.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 匹配请求参数，是一些权重，权重越大说明越不需要,反之，负数为需要
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerMatchReqDTO {
    /**
     * 性别权重
     */
    double sexWeight;

    /**
     * 年龄权重
     */
    double ageWeight;

    /**
     * 位置权重
     */
    double locationWeight;
}
