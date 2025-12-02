package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.biz.user.UserContext;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskSendTypeEnum;
import org.hgc.suts.volunteer.common.enums.VolunteerTaskStatusEnum;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.dao.entity.VolunteerTaskDO;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dto.req.VolunteerCreateTaskReq;
import org.hgc.suts.volunteer.service.VolunteerUserService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
* @author 谢毅强
* @description 针对表【volunteer_user】的数据库操作Service实现
* @createDate 2025-12-01 16:20:43
*/
@Service
@RequiredArgsConstructor
public class VolunteerUserServiceImpl extends ServiceImpl<VolunteerUserMapper, VolunteerUserDO> implements VolunteerUserService{


}




