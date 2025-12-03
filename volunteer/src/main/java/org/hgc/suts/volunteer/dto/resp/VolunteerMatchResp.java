package org.hgc.suts.volunteer.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 志愿者返回参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VolunteerMatchResp {

    /**
     * 志愿者ID
     */
    private Long id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 志愿者手机号
     */
    private String phone;

    /**
     * 匹配度
     */
    private Double matchDegree;
}
