package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.hgc.suts.volunteer.common.biz.user.UserContext;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.dao.entity.VolunteerRatingDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
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
@AllArgsConstructor
public class VolunteerRatingServiceImpl extends ServiceImpl<VolunteerRatingMapper, VolunteerRatingDO> implements VolunteerRatingService{

    private final VolunteerUserMapper volunteerUserMapper;
    @Override
    public void volunteerRating(VolunteerRatingReqDTO requestParam) {
        Long userId = UserContext.getUserId();
        if (userId==null){
            throw new ClientException("新增错误，UserId为空");
        }
        Long volunteerId = requestParam.getVolunteerId();
        // todo 可以先使用布隆过滤器过滤一次，防止大规模恶意请求打死数据库

        if (volunteerUserMapper.selectById(volunteerId) == null) {
            throw new ClientException("新增评价失败，该userId不存在");
        }
        VolunteerRatingDO volunteerRatingDO = BeanUtil.toBean(requestParam, VolunteerRatingDO.class);
        volunteerRatingDO.setUserId(userId);
        this.baseMapper.insert(volunteerRatingDO);

    }
}




