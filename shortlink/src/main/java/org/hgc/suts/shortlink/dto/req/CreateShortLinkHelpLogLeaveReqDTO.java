package org.hgc.suts.shortlink.dto.req;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class CreateShortLinkHelpLogLeaveReqDTO {


    /**
     * 外键，ShortLinkHelpDO
     */
    private Long requestId;

    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 离开时间
     */
    private Date leaveTime;
}
