package org.hgc.suts.volunteer.facade;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.volunteer.common.constant.RedisCacheConstant;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.common.result.Result;
import org.hgc.suts.volunteer.dao.entity.ShortLinkHelpDO;
import org.hgc.suts.volunteer.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReqDTO;
import org.hgc.suts.volunteer.dto.resp.ShortLinkHelpRespDTO;
import org.hgc.suts.volunteer.dto.resp.TargetRoomLinkInfoRespDTO;
import org.hgc.suts.volunteer.remote.ShortlinkRemoteService;
import org.hgc.suts.volunteer.service.VolunteerMatchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerHelpFacade {

    private final ShortlinkRemoteService shortlinkRemoteService;
    private final VolunteerMatchService volunteerMatchService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${sfu.front.domain.default}")
    private String stuFrontDomain;

    @Value("${sfu.apiUrl}")
    private String sfuAPIUrl;

    // 核心编排方法
    public TargetRoomLinkInfoRespDTO createAndDispatchHelp() {
        // 获取用户token
        String userToken = null;
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            userToken = request.getHeader("token");
        }
        // 创建房间
        HttpResponse response = HttpRequest.post(sfuAPIUrl)
                .header("token", userToken)
                .timeout(30000)
                .execute();
        // 返回房间链接和令牌
        String responseBody = response.body();
        JSONObject jsonObject = JSONUtil.parseObj(responseBody);
        if (jsonObject.getInt("code") != 200) {
            throw new ClientException("创建即时通讯房间错误");
        }
        JSONObject data = jsonObject.getJSONObject("data");
        String roomID=data.getStr("roomID");
        String targetRoomLink =String.format(stuFrontDomain,roomID);
        String authTicket=data.getStr("token");
        // 创建短链接
        ShortLinkHelpReqDTO shortLinkHelpReqDTO = ShortLinkHelpReqDTO.builder()
                .targetRoomLink(targetRoomLink)
                .authTicket(authTicket)
                .build();
        Result<ShortLinkHelpRespDTO> shortLinkHelpRespDTOResult = null;
        try {
            shortLinkHelpRespDTOResult = shortlinkRemoteService.createShortLinkHelp(shortLinkHelpReqDTO);
        } catch (Exception e) {
            throw new ClientException("当前访问过于繁忙，稍后再试");
        }
        String fullShortLink= shortLinkHelpRespDTOResult.getData().getFullShortLink();

        // 匹配志愿者(权重暂时写死)
        VolunteerMatchReqDTO volunteerMatchReqDTO = VolunteerMatchReqDTO.builder()
                .ageWeight(0.1)
                .locationWeight(0.1)
                .sexWeight(0.1)
                .build();
        List<Long> volunteerIdList = volunteerMatchService.matchVolunteer(volunteerMatchReqDTO);

        // 发送信息，暂时搁置，解决方案如下：
        // 先把志愿者的id列表和短信都发送到消息队列(此处使用消息队列是因为短链接的创建和志愿者匹配的响应速度都很快，而短信发送相对较慢，可以使用腾讯的短信api，也能使用其它的
        // 短链接大概在5ms，志愿者匹配初次24ms，命中热池7ms
        // 随后对短链接进行拼接，拼入验证码和对应的志愿者id
        String cacheValue = stringRedisTemplate.opsForValue().get(String.format(RedisCacheConstant.SHORT_LINK_GOTO, fullShortLink));
        ShortLinkHelpDO shortLinkHelpDO = JSON.parseObject(cacheValue, ShortLinkHelpDO.class);
        for (Long volunteerId : volunteerIdList) {
            if (shortLinkHelpDO != null) {
                String shortlink = fullShortLink + "/" + shortLinkHelpDO.getInviteCode() + "/" + volunteerId;
                log.info("发送短信成功，volunteerId:{}，短链接为: {}", volunteerId,shortlink);
            }
        }
        return TargetRoomLinkInfoRespDTO.builder()
                .targetRoomLink(targetRoomLink)
                .authTicket(authTicket)
                .build();
    }
}