package org.hgc.suts.volunteer.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLinkHelpReqDTO {

    /**
     * 房间链接
     */
    private String targetRoomLink;

    /**
     * 透传给IM服务的鉴权票据/Token
     */
    private String authTicket;

}
