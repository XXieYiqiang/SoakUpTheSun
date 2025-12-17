package org.hgc.suts.volunteer.controller;

import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.result.Result;
import org.hgc.suts.volunteer.common.web.Results;
import org.hgc.suts.volunteer.dto.req.*;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchRespDTO;
import org.hgc.suts.volunteer.service.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 志愿者控制层
 */
@RestController
@RequiredArgsConstructor
public class VolunteerUserController {

    private final VolunteerTaskService volunteerTaskService;

    private final VolunteerRatingService volunteerRatingService;

    private final VolunteerPrizesService volunteerPrizesService;

    private final VolunteerPrizesGrabService volunteerPrizesGrabService;

    private final VolunteerMatchService volunteerMatchService;

    /**
     * 新增志愿者任务
     */
    @PostMapping("/api/volunteer/volunteer-task/create")
    public Result<Void> createVolunteerTask(@RequestBody VolunteerCreateTaskReqDTO requestParam) {
        volunteerTaskService.createVolunteerTask(requestParam);
        return Results.success();
    }

    /**
     * 把志愿者加入活跃队列
     */
    @PostMapping("/api/volunteer/activateVolunteer")
    public Result<Void> activateVolunteer(@RequestParam Long volunteerId) {
        volunteerMatchService.activateVolunteer(volunteerId);
        return Results.success();
    }
    /**
     * 匹配志愿者
     */
    @PostMapping("/api/volunteer/matchVolunteer")
    public Result<List<Long>> matchVolunteer(@RequestBody VolunteerMatchReqDTO requestParam) {
        return Results.success(volunteerMatchService.matchVolunteer(requestParam));
    }

    /**
     * 志愿者评分
     */
    @PostMapping("/api/volunteer/volunteerRating")
    public Result<Void> createVolunteerTask(@RequestBody VolunteerRatingReqDTO requestParam) {
        volunteerRatingService.volunteerRating(requestParam);
        return Results.success();
    }

    /**
     * 志愿者奖品分发
     */
    @PostMapping("/api/volunteer/volunteerPrizesSend")
    public Result<Void> volunteerPrizesSend(@RequestBody VolunteerPrizeDistributionReqDTO requestParam) {
        volunteerPrizesService.volunteerPrizeDistribution(requestParam);
        return Results.success();
    }

    /**
     * 志愿者奖品秒杀
     */
    @PostMapping("/api/volunteer/redeemVolunteerPrizes")
    public Result<Void> redeemVolunteerPrizes(@RequestBody redeemVolunteerPrizesDTO requestParam) {
        volunteerPrizesGrabService.redeemVolunteerPrizes(requestParam);
        return Results.success();
    }

}
