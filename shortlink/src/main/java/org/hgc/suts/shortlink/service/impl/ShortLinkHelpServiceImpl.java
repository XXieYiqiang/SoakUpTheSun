package org.hgc.suts.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hgc.suts.shortlink.common.constant.RedisCacheConstant;
import org.hgc.suts.shortlink.common.enums.ShortLinkHelpStatusEnum;
import org.hgc.suts.shortlink.common.exception.ClientException;
import org.hgc.suts.shortlink.common.exception.ServiceException;
import org.hgc.suts.shortlink.dao.entity.ShortLinkHelpDO;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpRestoreReqDTO;
import org.hgc.suts.shortlink.dto.resp.ShortLinkHelpRespDTO;
import org.hgc.suts.shortlink.service.ShortLinkHelpService;
import org.hgc.suts.shortlink.dao.mapper.ShortLinkHelpMapper;
import org.hgc.suts.shortlink.utils.RandomUtils;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
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
    private final RedissonClient redissonClient;
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
        stringRedisTemplate.opsForValue().set(
                shortlinkGoToKey,
                JSON.toJSONString(shortLinkHelpDO),
                EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );
        // 加入布隆过滤器中
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return BeanUtil.toBean(shortLinkHelpDO,ShortLinkHelpRespDTO.class);
    }




    @SneakyThrows
    @Override
    public void restoreUrl(ShortLinkHelpRestoreReqDTO requestParm, ServletRequest request, ServletResponse response) {


        // 初步校验，防止恶意攻击
        if (requestParm.getShortUri() == null || requestParm.getShortUri().length() != 6 || !isValidFormat(requestParm.getShortUri())) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName + serverPort + "/" + requestParm.shortUri;

        String cacheValue = stringRedisTemplate.opsForValue().get(String.format(RedisCacheConstant.SHORT_LINK_GOTO, fullShortUrl));
        if (StrUtil.isNotBlank(cacheValue)) {

            ShortLinkHelpDO shortLinkHelpDO = JSON.parseObject(cacheValue, ShortLinkHelpDO.class);
            // 验证并跳转
            validateAndRedirect(shortLinkHelpDO,requestParm,response);
            return;
        }
        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(RedisCacheConstant.SHORT_LINK_GOTO_IS_NULL_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        RLock lock = redissonClient.getLock(String.format(RedisCacheConstant.SHORT_LINK_GOTO_LOCK_KEY, fullShortUrl));
        lock.lock();
        try {
            cacheValue = stringRedisTemplate.opsForValue().get(String.format(RedisCacheConstant.SHORT_LINK_GOTO, fullShortUrl));
            if (StrUtil.isNotBlank(cacheValue)) {

                ShortLinkHelpDO shortLinkHelpDO = JSON.parseObject(cacheValue, ShortLinkHelpDO.class);
                // 验证并跳转
                validateAndRedirect(shortLinkHelpDO,requestParm,response);
                return;
            }
            gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(RedisCacheConstant.SHORT_LINK_GOTO_IS_NULL_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkHelpDO> linkQueryWrapper = Wrappers.lambdaQuery(ShortLinkHelpDO.class)
                    .eq(ShortLinkHelpDO::getFullShortLink, fullShortUrl);
            ShortLinkHelpDO shortLinkHelpDO = shortLinkHelpMapper.selectOne(linkQueryWrapper);
            if (shortLinkHelpDO == null || shortLinkHelpDO.getExpireTime().before(new Date())) {
                stringRedisTemplate.opsForValue().set(String.format(RedisCacheConstant.SHORT_LINK_GOTO_IS_NULL_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            String shortlinkGoToKey = String.format(RedisCacheConstant.SHORT_LINK_GOTO,fullShortUrl);
            stringRedisTemplate.opsForValue().set(
                    shortlinkGoToKey,
                    JSON.toJSONString(shortLinkHelpDO),
                    EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
            // 验证验证码
            validateAndRedirect(shortLinkHelpDO,requestParm,response);
        } finally {
            lock.unlock();
        }
    }

    // 验证并跳转
    private void validateAndRedirect(ShortLinkHelpDO shortLinkHelpDO, ShortLinkHelpRestoreReqDTO requestParm, ServletResponse response) throws IOException {
        if (!shortLinkHelpDO.getInviteCode().equals(requestParm.inviteCode)) {
            throw new ClientException("验证码错误，跳转失败");
        }
        // 验证状态,如果处于结束态
        if (shortLinkHelpDO.getStatus().equals(ShortLinkHelpStatusEnum.ENDED.getStatus())||shortLinkHelpDO.getExpireTime().before(new Date())) {
            // todo 是否愿意继续匹配.
            throw new ClientException("该请求协助已经结束");

        }
        String ActualLink = UriComponentsBuilder.fromHttpUrl(shortLinkHelpDO.getTargetRoomLink())
                .queryParam("inviteCode", shortLinkHelpDO.getInviteCode())
                .queryParam("authTicket", shortLinkHelpDO.getAuthTicket())
                .queryParam("volunteerId", requestParm.volunteerId)
                .build()
                .toUriString();
        //todo 记录志愿者协助
        ((HttpServletResponse) response).sendRedirect(ActualLink);
    }

    private boolean isValidFormat(String code) {
        // 简单校验
        return code.matches("^[a-zA-Z0-9]+$");
    }

}




