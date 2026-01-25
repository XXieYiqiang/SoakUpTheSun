package org.hgc.suts.volunteer.dto.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class redeemVolunteerPrizesDTO {

    /**
     * 志愿者id
     */
    private Long volunteerId;

    /**
     * 奖品id
     */
    private Long prizesId;
}
