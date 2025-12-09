package org.hgc.suts.volunteer.mq.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hgc.suts.volunteer.dto.resp.VolunteerPrizesRespDTO;

/**
 * 同步发奖品到数据库中
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerPrizesGrabDBSyncEvent {
    /**
     * 志愿者id
     */
    Long volunteerId;

    /**
     * 奖品信息
     */
    VolunteerPrizesRespDTO volunteerPrizesDBSyncDTO;
}
