package org.hgc.suts.volunteer.dto.req;


import lombok.Data;

@Data
public class VolunteerRatingReqDTO {
    /**
     * 志愿者id
     */
    private Long userId;

    /**
     * 附加分(0无 1有)
     */
    private Integer rating;
}
