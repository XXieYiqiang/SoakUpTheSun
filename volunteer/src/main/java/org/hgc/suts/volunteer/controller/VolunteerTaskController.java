package org.hgc.suts.volunteer.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.result.Result;
import org.hgc.suts.volunteer.common.web.Results;
import org.hgc.suts.volunteer.dao.mapper.VolunteerTaskMapper;
import org.hgc.suts.volunteer.dto.req.VolunteerCreateTaskReq;
import org.hgc.suts.volunteer.service.VolunteerTaskService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


/**
 * 志愿者任务控制层
 */
@RestController
@RequiredArgsConstructor
public class VolunteerTaskController {

    private final VolunteerTaskService volunteerTaskService;

    /**
     * 新增志愿者任务
     */
    @PostMapping("/api/volunteer/volunteer-task/create")
    public Result<Void> createVolunteerTask(@RequestBody VolunteerCreateTaskReq requestParam) {
        volunteerTaskService.createVolunteerTask(requestParam);
        return Results.success();
    }
}
