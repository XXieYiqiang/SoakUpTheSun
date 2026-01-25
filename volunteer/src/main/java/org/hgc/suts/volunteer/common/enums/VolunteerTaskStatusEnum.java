

package org.hgc.suts.volunteer.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public enum VolunteerTaskStatusEnum {

    /**
     * 待执行
     */
    PENDING(0),

    /**
     * 执行中
     */
    IN_PROGRESS(1),

    /**
     * 执行失败
     */
    FAILED(2),

    /**
     * 执行成功
     */
    SUCCESS(3),

    /**
     * 取消
     */
    CANAL(4);

    @Getter
    private final int status;
}
