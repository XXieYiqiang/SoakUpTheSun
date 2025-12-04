package org.hgc.suts.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.result.Result;
import org.hgc.suts.volunteer.common.web.Results;
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReq;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchResp;
import org.hgc.suts.volunteer.service.VolunteerTaskService;
import org.hgc.suts.volunteer.service.VolunteerUserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 志愿者控制层
 */
@RestController
@RequiredArgsConstructor
public class VolunteerUserController {

    private final VolunteerUserService volunteerUserService;

    /**
     * 匹配志愿者
     */
    @PostMapping("/api/volunteer/matchVolunteer")
    public Result<List<VolunteerMatchResp>> createVolunteerTask(@RequestBody VolunteerMatchReq requestParam) {

        return Results.success(volunteerUserService.matchVolunteer(requestParam));
    }
}
