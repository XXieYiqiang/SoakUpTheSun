package org.hgc.suts.shortlink.dto.req;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
@Data
public class CreateShortLinkHelpLogReqDTO {


    /**
     * 外键，ShortLinkHelpDO
     */
    private Long requestId;

    /**
     * 志愿者ID
     */
    private Long volunteerId;

    /**
     * 进入房间时间
     */
    private Date joinTime;

}
