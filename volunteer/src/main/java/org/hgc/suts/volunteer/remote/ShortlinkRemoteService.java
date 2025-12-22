package org.hgc.suts.volunteer.remote;



import org.hgc.suts.volunteer.common.result.Result;
import org.hgc.suts.volunteer.dto.req.ShortLinkHelpReqDTO;
import org.hgc.suts.volunteer.dto.resp.ShortLinkHelpRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "shortlink")
public interface ShortlinkRemoteService {
    @PostMapping("/api/shortlinkHelp/create")
    public Result<ShortLinkHelpRespDTO> createShortLinkHelp(@RequestBody ShortLinkHelpReqDTO requestParam);
}
