package org.hgc.suts.shortlink.controller;


import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.shortlink.common.result.Result;
import org.hgc.suts.shortlink.common.web.Results;
import org.hgc.suts.shortlink.dto.req.CreateShortLinkHelpLogLeaveReqDTO;
import org.hgc.suts.shortlink.dto.req.CreateShortLinkHelpLogReqDTO;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpRestoreReqDTO;
import org.hgc.suts.shortlink.dto.resp.ShortLinkHelpRespDTO;
import org.hgc.suts.shortlink.service.ShortLinkHelpLogService;
import org.hgc.suts.shortlink.service.ShortLinkHelpService;
import org.springframework.web.bind.annotation.*;

/**
 * 短链接帮助日志控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkHelpLogController {


    private final ShortLinkHelpLogService shortLinkHelpLogService;

    /**
     * 新增短链接日志
     */
    @PostMapping("/api/shortlinkHelpLog/create")
    public Result<Void> createShortLinkHelp(@RequestBody CreateShortLinkHelpLogReqDTO requestParam) {
        shortLinkHelpLogService.createShortLinkHelpLog(requestParam);
        return Results.success();
    }

    /**
     * 修改短链接日志离开时间
     */
    @PostMapping("/api/shortlinkHelpLog/createLeave")
    public Result<Void> createShortLinkHelpLeave(@RequestBody CreateShortLinkHelpLogLeaveReqDTO requestParam) {
        shortLinkHelpLogService.updateShortLinkHelpLeave(requestParam);
        return Results.success();
    }


}
