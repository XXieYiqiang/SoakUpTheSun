package org.hgc.suts.volunteer.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;

import java.io.Serializable;
import java.util.List;

/**
 * 用于批量新增入MYSQL时，同步新增入es
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerPrizesSendEvent implements Serializable {
    
    private Long batchId; 
    private List<Long> userList;
}