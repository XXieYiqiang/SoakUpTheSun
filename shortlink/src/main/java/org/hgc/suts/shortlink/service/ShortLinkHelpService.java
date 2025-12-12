package org.hgc.suts.shortlink.service;

import org.hgc.suts.shortlink.dao.entity.ShortLinkHelpDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.shortlink.dto.resp.ShortLinkHelpRespDTO;

/**
* @author 谢毅强
* @description 针对表【short_link_help(求助短链路由表)】的数据库操作Service
* @createDate 2025-12-11 22:15:57
*/
public interface ShortLinkHelpService extends IService<ShortLinkHelpDO> {

    /**
     * 创建短链接
     * @param requestParam 请求参数
     * @return 返回短链接
     */
    ShortLinkHelpRespDTO createShortLinkHelp(ShortLinkHelpReqDTO requestParam);

}
