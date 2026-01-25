package org.hgc.suts.shortlink.dto.req;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ShortLinkHelpRestoreReqDTO {

    /**
     * 短链接
     */
    public String shortUri;

    /**
     * 邀请码
     */
    public String inviteCode;

    /**
     * 志愿者id
     */
    public Long volunteerId;
}
