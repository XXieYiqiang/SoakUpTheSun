package org.hgc.suts.volunteer.service;

import org.hgc.suts.volunteer.dao.entity.VolunteerRatingDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.volunteer.dto.req.VolunteerRatingReqDTO;

/**
* @author 谢毅强
* @description 针对表【volunteer_rating】的数据库操作Service
* @createDate 2025-12-04 19:25:59
*/
public interface VolunteerRatingService extends IService<VolunteerRatingDO> {


    /**
     * 志愿者评分
     * @param requestParam 志愿者的评分和id
     */
    void volunteerRating(VolunteerRatingReqDTO requestParam);

}
