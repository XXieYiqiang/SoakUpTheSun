package org.hgc.suts.gateway.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TargetRoomLinkInfoRespDTO {
    /**
     * 目标房间链接
     */
    String targetRoomLink;
    /**
     * 校验
     */
    String authTicket;
}
