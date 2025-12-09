package org.hgc.suts.volunteer.dto.resp;

import lombok.Data;

import java.util.Date;
@Data
public class VolunteerPrizesRespDTO {
    private Long id;

    /**
     * 奖品名称
     */
    private String name;

    /**
     * 有效期开始时间
     */
    private Date validStartTime;

    /**
     * 有效期结束时间
     */
    private Date validEndTime;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 状态
     */
    private Integer status;
}
