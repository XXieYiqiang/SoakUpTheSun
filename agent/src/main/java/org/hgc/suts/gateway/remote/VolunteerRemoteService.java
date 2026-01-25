package org.hgc.suts.gateway.remote;



import org.hgc.suts.gateway.common.result.Result;
import org.hgc.suts.gateway.dto.resp.TargetRoomLinkInfoRespDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 志愿服务远程客户端
 */
@FeignClient(value = "volunteer")
public interface VolunteerRemoteService {
    @PostMapping("/api/volunteer/help/create")
    Result<TargetRoomLinkInfoRespDTO> createHelpTask();
}