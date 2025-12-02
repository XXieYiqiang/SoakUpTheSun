
package org.hgc.suts.volunteer.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推送任务执行事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerTaskExecuteEvent {

    /**
     * 推送任务id
     */
    private Long volunteerTaskId;
}
