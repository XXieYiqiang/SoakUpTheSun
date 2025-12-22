package org.hgc.suts.volunteer.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ShortLinkHelpRespDTO {


    /**
     * 短链接
     */
    private String fullShortLink;

    /**
     * 过期时间
     */
    private Date expireTime;


}
