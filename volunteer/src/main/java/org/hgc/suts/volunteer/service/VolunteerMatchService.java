package org.hgc.suts.volunteer.service;

import org.hgc.suts.volunteer.dto.req.VolunteerMatchReqDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchRespDTO;

import java.util.List;

public interface VolunteerMatchService {
    /**
     * 激活志愿者
     * @param volunteerId 志愿者id
     */
    void activateVolunteer(Long volunteerId);

    /**
     * 匹配志愿者
     * @param requestParam 权重
     * @return 志愿者id列表
     */
    List<Long> matchVolunteer(VolunteerMatchReqDTO requestParam);

}
