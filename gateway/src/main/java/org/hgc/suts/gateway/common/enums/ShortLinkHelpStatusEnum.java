

package org.hgc.suts.gateway.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public enum ShortLinkHelpStatusEnum {

    /**
     * 等待中
     */
    WAITING(0),

    /**
     * 进行中
     */
    IN_PROGRESS(1),

    /**
     * 已结束
     */
    ENDED(2),;

    @Getter
    private final int status;
}
