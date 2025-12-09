package org.hgc.suts.volunteer.service;

import org.hgc.suts.volunteer.dao.entity.VolunteerPrizesGrabDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.volunteer.dto.req.redeemVolunteerPrizesDTO;

/**
* @author 谢毅强
* @description 针对表【volunteer_prizes_grab】的数据库操作Service
* @createDate 2025-12-09 14:45:16
*/
public interface VolunteerPrizesGrabService extends IService<VolunteerPrizesGrabDO> {

    /**
     * 志愿者秒杀奖品
     * @param requestParam 请求奖品id，志愿者id
     */
    void redeemVolunteerPrizes(redeemVolunteerPrizesDTO requestParam);
}
