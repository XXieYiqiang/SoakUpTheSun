package org.hgc.suts.shortlink.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.bouncycastle.jcajce.provider.symmetric.AES;
import org.hgc.suts.shortlink.dao.entity.ShortLinkHelpLogDO;
import org.hgc.suts.shortlink.dto.req.CreateShortLinkHelpLogLeaveReqDTO;
import org.hgc.suts.shortlink.dto.req.CreateShortLinkHelpLogReqDTO;
import org.hgc.suts.shortlink.service.ShortLinkHelpLogService;
import org.hgc.suts.shortlink.dao.mapper.ShortLinkHelpLogMapper;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
* @author 谢毅强
* @description 针对表【t_short_link_help_log(服务接单记录)】的数据库操作Service实现
* @createDate 2025-12-12 21:11:22
*/
@Service
public class ShortLinkHelpLogServiceImpl extends ServiceImpl<ShortLinkHelpLogMapper, ShortLinkHelpLogDO> implements ShortLinkHelpLogService {

    @Override
    public void createShortLinkHelpLog(CreateShortLinkHelpLogReqDTO requestParam) {
        //todo 利用布隆过滤器查volunteer情况
        ShortLinkHelpLogDO shortLinkHelpLogDO = ShortLinkHelpLogDO.builder()
                .requestId(requestParam.getRequestId())
                .volunteerId(requestParam.getVolunteerId())
                .joinTime(new Date())
                .build();
        baseMapper.insert(shortLinkHelpLogDO);
    }

    @Override
    public void updateShortLinkHelpLeave(CreateShortLinkHelpLogLeaveReqDTO requestParam) {
        //todo 利用布隆过滤器查volunteer情况
        LambdaUpdateWrapper<ShortLinkHelpLogDO> shortLinkHelpLogDOLambdaUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkHelpLogDO.class)
                .eq(ShortLinkHelpLogDO::getRequestId, requestParam.getRequestId())
                .eq(ShortLinkHelpLogDO::getVolunteerId, requestParam.getVolunteerId())
                .set(ShortLinkHelpLogDO::getLeaveTime, requestParam.getLeaveTime());
        baseMapper.update(shortLinkHelpLogDOLambdaUpdateWrapper);
    }

}




