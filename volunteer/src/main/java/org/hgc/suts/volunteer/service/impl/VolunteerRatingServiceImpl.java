package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hgc.suts.volunteer.dao.entity.VolunteerRatingDO;
import org.hgc.suts.volunteer.dto.req.VolunteerRatingReqDTO;
import org.hgc.suts.volunteer.service.VolunteerRatingService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerRatingMapper;
import org.springframework.stereotype.Service;

/**
* @author 谢毅强
* @description 针对表【volunteer_rating】的数据库操作Service实现
* @createDate 2025-12-04 19:25:59
*/
@Service
public class VolunteerRatingServiceImpl extends ServiceImpl<VolunteerRatingMapper, VolunteerRatingDO> implements VolunteerRatingService{

    @Override
    public void volunteerRating(VolunteerRatingReqDTO volunteerRatingReqDTO) {

        VolunteerRatingDO volunteerRatingDO = BeanUtil.toBean(volunteerRatingReqDTO, VolunteerRatingDO.class);
        this.save(volunteerRatingDO);

    }
}




