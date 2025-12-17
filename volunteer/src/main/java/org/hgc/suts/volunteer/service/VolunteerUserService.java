package org.hgc.suts.volunteer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReqDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchRespDTO;

import java.util.List;

/**
* @author 谢毅强
* @description 针对表【volunteer_user】的数据库操作Service
* @createDate 2025-12-01 16:20:43
*/
public interface VolunteerUserService extends IService<VolunteerUserDO> {

}
