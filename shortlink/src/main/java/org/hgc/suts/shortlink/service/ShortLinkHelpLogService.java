package org.hgc.suts.shortlink.service;

import org.hgc.suts.shortlink.dao.entity.ShortLinkHelpLogDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.shortlink.dto.req.CreateShortLinkHelpLogLeaveReqDTO;
import org.hgc.suts.shortlink.dto.req.CreateShortLinkHelpLogReqDTO;

/**
* @author 谢毅强
* @description 针对表【t_short_link_help_log(服务接单记录)】的数据库操作Service
* @createDate 2025-12-12 21:11:22
*/
public interface ShortLinkHelpLogService extends IService<ShortLinkHelpLogDO> {
    /**
     * 创建记录志愿者帮助记录
     * @param requestParam 请求参数
     */
    void createShortLinkHelpLog(CreateShortLinkHelpLogReqDTO requestParam);

    /**
     * 修改离开时间
     * @param requestParam 请求参数
     */
    void updateShortLinkHelpLeave(CreateShortLinkHelpLogLeaveReqDTO requestParam);
}
