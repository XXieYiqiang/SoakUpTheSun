

package org.hgc.suts.volunteer.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public enum VolunteerPrizesSendTypeEnum {

    /**
     * 未发放
     */
    PENDING(0),

    /**
     * 发放中
     */
    IN_PROGRESS(1),

    /**
     * 发放成功
     */
    SUCCESS(2);

    @Getter
    private final int status;
}
