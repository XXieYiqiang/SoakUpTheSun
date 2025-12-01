package org.hgc.suts.volunteer.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum VolunteerTaskSendTypeEnum {

    /**
     * 立即发送
     */
    IMMEDIATE(0),

    /**
     * 定时发送
     */
    SCHEDULED(1);

    @Getter
    private final int type;
}