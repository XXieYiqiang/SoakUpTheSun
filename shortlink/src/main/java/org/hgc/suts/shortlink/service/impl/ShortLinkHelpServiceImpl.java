package org.hgc.suts.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrBuilder;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.shortlink.common.constant.RedisCacheConstant;
import org.hgc.suts.shortlink.common.exception.ServiceException;
import org.hgc.suts.shortlink.dao.entity.ShortLinkHelpDO;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.shortlink.dto.resp.ShortLinkHelpRespDTO;
import org.hgc.suts.shortlink.service.ShortLinkHelpService;
import org.hgc.suts.shortlink.dao.mapper.ShortLinkHelpMapper;
import org.hgc.suts.shortlink.utils.RandomUtils;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
* @author 谢毅强
* @description 针对表【short_link_help(求助短链路由表)】的数据库操作Service实现
* @createDate 2025-12-11 22:15:57
*/
@Service
@RequiredArgsConstructor
public class ShortLinkHelpServiceImpl extends ServiceImpl<ShortLinkHelpMapper, ShortLinkHelpDO> implements ShortLinkHelpService{


    private final ShortLinkCodeService shortLinkCodeService;
    private final RandomUtils randomUtils;
    private final ShortLinkHelpMapper shortLinkHelpMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;

    @Value("${short-link.domain.default}")
    private String createShortLinkDefaultDomain;
    // 定义过期时间常量：10分钟
    private static final long EXPIRE_MINUTES = 10;

    @Override
    public ShortLinkHelpRespDTO createShortLinkHelp(ShortLinkHelpReqDTO requestParam) {

        // 短链码
        String code = shortLinkCodeService.takeCode();
        String fullShortUrl = StrBuilder.create(createShortLinkDefaultDomain)
                .append("/")
                .append(code)
                .toString();
        // 构建帮助链接

        ShortLinkHelpDO shortLinkHelpDO = ShortLinkHelpDO.builder()
                .shortCode(code)
                .fullShortLink(fullShortUrl)
                .targetRoomLink(requestParam.getTargetRoomLink())
                .authTicket(requestParam.getAuthTicket())
                .inviteCode(randomUtils.generateInviteCode(6))
                .expireTime(DateUtil.offsetMinute(new Date(), 10))
                .status(0)
                .build();

        try {
            shortLinkHelpMapper.insert(shortLinkHelpDO);
        } catch (DuplicateKeyException ex) {
            if (!shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl)) {
                shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
            }
            throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
        }
        // 加入redis中，下次访问加速
        String shortlinkGoToKey = String.format(RedisCacheConstant.SHORT_LINK_GOTO,fullShortUrl) ;
        stringRedisTemplate.opsForValue().set(shortlinkGoToKey, requestParam.getTargetRoomLink(), EXPIRE_MINUTES, TimeUnit.MINUTES);
        // 加入布隆过滤器中
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return BeanUtil.toBean(shortLinkHelpDO,ShortLinkHelpRespDTO.class);
    }


}




