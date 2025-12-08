package org.hgc.suts.volunteer.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;

import java.io.Serializable;
import java.util.List;

/**
 * 用于把发放奖品记录保存到数据库中
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerPrizesSendEvent implements Serializable {
    
    private Long batchId; 
    private List<Long> userList;
}