package org.hgc.suts.volunteer.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.yetus.audience.InterfaceAudience;

/**
 * 匹配请求参数，是一些权重，权重越大说明越不需要,反之，负数为需要
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VolunteerMatchReqDTO {
    /**
     * 性别权重
     */
    @Schema(description = "性别权重 (默认0.5)", example = "0.5")
    private Double sexWeight = 0.5;


    /**
     * 年龄权重
     */
    @Schema(description = "年龄权重 (默认0.3)", example = "0.3")
    private Double ageWeight = 0.3;

    /**
     * 位置权重
     */
    @Schema(description = "位置权重 (默认0.01)", example = "0.01")
    private Double locationWeight = 0.01;
}
