package org.hgc.suts.shortlink.controller;


import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.shortlink.common.result.Result;
import org.hgc.suts.shortlink.common.web.Results;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.shortlink.dto.req.ShortLinkHelpRestoreReqDTO;
import org.hgc.suts.shortlink.dto.resp.ShortLinkHelpRespDTO;
import org.hgc.suts.shortlink.service.ShortLinkHelpService;
import org.springframework.web.bind.annotation.*;

/**
 * 短链接创建控制层
 */
@RestController
@RequiredArgsConstructor
public class ShortLinkHelpController {


    private final ShortLinkHelpService shortLinkHelpService;

    /**
     * 新增短链接
     */
    @PostMapping("/api/shortlinkHelp/create")
    public Result<ShortLinkHelpRespDTO> createShortLinkHelp(@RequestBody ShortLinkHelpReqDTO requestParam) {

        return Results.success(shortLinkHelpService.createShortLinkHelp(requestParam));
    }

    @GetMapping("/restore/{short-uri}/{invite-code}/{volunteer-id}")
    public void restoreUrl(@PathVariable("short-uri") String shortUri,@PathVariable("invite-code")String inviteCode,@PathVariable("volunteer-id")Long volunteerId,ServletRequest request, ServletResponse response){
        ShortLinkHelpRestoreReqDTO requestParm = ShortLinkHelpRestoreReqDTO.builder()
                .shortUri(shortUri)
                .inviteCode(inviteCode)
                .volunteerId(volunteerId)
                .build();
        shortLinkHelpService.restoreUrl(requestParm, request, response);
    }

}
