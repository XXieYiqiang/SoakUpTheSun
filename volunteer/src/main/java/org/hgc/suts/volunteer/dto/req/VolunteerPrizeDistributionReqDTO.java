package org.hgc.suts.volunteer.dto.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 请求分发奖品实体
 */
@Data
public class VolunteerPrizeDistributionReqDTO {
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

}
