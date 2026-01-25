package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.volunteer.common.biz.user.UserContext;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReqDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchRespDTO;
import org.hgc.suts.volunteer.service.VolunteerUserService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author 谢毅强
* @description 针对表【volunteer_user】的数据库操作Service实现
* @createDate 2025-12-01 16:20:43
*/
@Service
@RequiredArgsConstructor
public class VolunteerUserServiceImpl extends ServiceImpl<VolunteerUserMapper, VolunteerUserDO> implements VolunteerUserService{

}




