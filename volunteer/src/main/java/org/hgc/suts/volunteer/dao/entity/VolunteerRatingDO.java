package org.hgc.suts.volunteer.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 
 * @TableName volunteer_rating
 */
@TableName(value ="volunteer_rating")
@Data
public class VolunteerRatingDO {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 志愿者id
     */
    private Long userId;

    /**
     * 附加分(0无 1有)
     */
    private Integer rating;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 逻辑删除 0/1 存在/删除
     */
    private Integer delFlag;
}