package org.hgc.suts.volunteer.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis 志愿者抢奖品时的状态返回
 */
@RequiredArgsConstructor
public enum VolunteerPrizesGrabStutsEnum {

    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 库存不足
     */
    STOCK_INSUFFICIENT(1, "奖品被领完啦"),

    /**
     * 用户只能领一张
     */
    LIMIT_REACHED(2, "每个用户只能领取一张");

    @Getter
    private final long code;
    @Getter
    private final String message;

    /**
     * 根据 code 找到对应的枚举实例判断是否成功标识
     *
     * @param code 要查找的编码
     * @return 是否成功标识
     */
    public static boolean isFail(long code) {
        for (VolunteerPrizesGrabStutsEnum status : values()) {
            if (status.code == code) {
                return status != SUCCESS;
            }
        }
        return false;
    }

    /**
     * 根据 type 找到对应的枚举实例
     *
     * @param code 要查找的编码
     * @return 对应的枚举实例
     */
    public static String fromType(long code) {
        for (VolunteerPrizesGrabStutsEnum method : VolunteerPrizesGrabStutsEnum.values()) {
            if (method.getCode() == code) {
                return method.getMessage();
            }
        }
        throw new IllegalArgumentException("Invalid code: " + code);
    }
}
