package org.hgc.suts.volunteer.service;

import org.hgc.suts.volunteer.dao.entity.VolunteerPrizesDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.volunteer.dto.req.VolunteerPrizeDistributionReqDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerPrizesRespDTO;

/**
* @author 谢毅强
* @description 针对表【volunteer_prizes(志愿者奖品表)】的数据库操作Service
* @createDate 2025-12-07 14:34:07
*/
public interface VolunteerPrizesService extends IService<VolunteerPrizesDO> {


    /**
     * 创建分发奖品列表
     */
    void createVolunteerPrizes(VolunteerPrizesDO volunteerPrizes);

    /**
     * 查找奖品信息
     * @param prizesId 奖品id
     * @return 返回奖品信息
     */
    VolunteerPrizesRespDTO findVolunteerPrizes(Long prizesId);

    /**
     * 给评分排名前？%分发奖品
     */
    void volunteerPrizeDistribution(VolunteerPrizeDistributionReqDTO requestParam);
}
