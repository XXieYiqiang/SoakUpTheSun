package org.hgc.suts.volunteer.controller;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.result.Result;
import org.hgc.suts.volunteer.common.web.Results;
import org.hgc.suts.volunteer.dto.req.VolunteerCreateTaskReqDTO;
import org.hgc.suts.volunteer.facade.VolunteerHelpFacade;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
/**
 * 志愿者请求服务编排
 */
public class VolunteerHelpFacadeController {
    private final VolunteerHelpFacade volunteerHelpFacade;

    /**
     * 创建请求
     */
    @PostMapping("/api/volunteer/help/create")
    public Result<Void> createHelpTask() {

        volunteerHelpFacade.createAndDispatchHelp();
        return Results.success();
    }

}
