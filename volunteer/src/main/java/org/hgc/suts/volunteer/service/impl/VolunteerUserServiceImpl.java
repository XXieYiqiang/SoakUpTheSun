package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReq;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchResp;
import org.hgc.suts.volunteer.service.VolunteerUserService;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
* @author 谢毅强
* @description 针对表【volunteer_user】的数据库操作Service实现
* @createDate 2025-12-01 16:20:43
*/
@Service
@RequiredArgsConstructor
public class VolunteerUserServiceImpl extends ServiceImpl<VolunteerUserMapper, VolunteerUserDO> implements VolunteerUserService{


    private final ElasticsearchClient elasticsearchClient;
    private static final String ES_INDEX_NAME = "volunteer_index";

    @Override
    public List<VolunteerMatchResp> matchVolunteer(VolunteerMatchReq requestParam) {

        // 1. 当前用户的数据参数
        // 计算当前用户的年龄
        int userAge = Period.between(Objects.requireNonNull(UserContext.getBirthday()).toLocalDate(), LocalDate.ofEpochDay(LocalDate.now().getYear())).getYears();

        // 解析当前用户的经纬度 (假设 location 格式为 "lon,lat")
        String[] locationParts = Objects.requireNonNull(UserContext.getLocation()).split(",");
        double userLon = Double.parseDouble(locationParts[0]);
        double userLat = Double.parseDouble(locationParts[1]);

        // 2. 构建 评分 脚本参数 map
        Map<String, JsonData> params = new HashMap<>();
        
        params.put("sexWeight", JsonData.of(Math.abs(requestParam.getSexWeight()))); // 性别权重
        params.put("ageWeight", JsonData.of(Math.abs(requestParam.getAgeWeight()))); // 年龄权重
        params.put("locationWeight", JsonData.of(Math.abs(requestParam.getLocationWeight()))); // 距离权重

        params.put("userSex", JsonData.of(UserContext.getUserSex()));
        params.put("userAge", JsonData.of(userAge));
        params.put("userLon", JsonData.of(userLon));
        params.put("userLat", JsonData.of(userLat));

        // 今年是几年
        params.put("currentYear", JsonData.of(LocalDate.now().getYear()));

        // 3. 定义 评分 脚本
        String scriptSource =
                        "double sexWeight = params.sexWeight; " +
                        "double ageWeight = params.ageWeight; " +
                        "double locationWeight = params.locationWeight; " +

                        // 计算性别差异 (0或1)
                        "double sexVal = doc['sex'].size() > 0 ? doc['sex'].value : 0; " +
                        "double sexDiff = Math.pow(params.userSex - sexVal, 2); " +

                        // 计算年龄差异
                        "int docYear = doc['birthday'].size() > 0 ? doc['birthday'].value.year : params.currentYear; " +
                        "int docAge = params.currentYear - docYear; " +
                        "double ageDiff = Math.pow(params.userAge - docAge, 2); " +

                        // 计算距离差异 (米)
                        "double locDiff = 0.0; " +
                        "if (doc['location'].size() > 0) { " +
                        "   locDiff = doc['location'].arcDistance(params.userLat, params.userLon); " +
                        "} " +

                        // f(x)
                        "double fx = sexWeight * sexDiff + ageWeight * ageDiff + locationWeight * locDiff; " +
                        "double K_SCALE = 0.005; " +

                        "return Math.exp(-K_SCALE * fx);";
        // 4. 构建查询
        try {

            Script rankingScript = new Script.Builder()
                    .inline(in -> in
                            .source(scriptSource)
                            .params(params)
                    )
                    .build();

            // 记录当前时间
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            //设置过滤条件
            Query activeDataQuery = new Query.Builder()
                    .bool(b -> b
                            // 条件1：未删除
                            .must(m -> m
                                    .term(t -> t
                                            .field("delFlag")
                                            .value(0)
                                    )
                            )
//                            // 条件2：startTime <= nowTime
//                            .must(m -> m
//                                    .range(r -> r
//                                            .field("startTime")
//                                            .lte(JsonData.of(nowTime))
//                                    )
//                            )
//                            // 条件3：endTime >= nowTime
//                            .must(m -> m
//                                    .range(r -> r
//                                            .field("endTime")
//                                            .gte(JsonData.of(nowTime))
//                                    )
//                            )
                    )
                    .build();

            // 执行查询
            SearchResponse<VolunteerUserDO> response = elasticsearchClient.search(s -> s
                            .index(ES_INDEX_NAME)
                            .size(10) // 取前10
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            // 根据过滤器查询
                                            .query(activeDataQuery)

                                            // 根据
                                            .functions(f -> f
                                                    .scriptScore(ss -> ss.script(rankingScript))
                                            )

                                            // 使用脚本计算出的分数直接替换原有分数(不被es内部评分影响)
                                            .boostMode(FunctionBoostMode.Replace)
                                    )
                            ),
                    VolunteerUserDO.class
            );
            // 5. 提取结果
            return response.hits().hits().stream()
                    .map(hit -> {
                        VolunteerUserDO userDO = hit.source();
                        if (userDO == null) {
                            return null;
                        }

                        // 转换结果
                        VolunteerMatchResp resp = BeanUtil.toBean(userDO, VolunteerMatchResp.class);
                        // 记录每个人的偏差值
                        if (hit.score() != null) {
                            resp.setMatchDegree(hit.score());
                        }
                        return resp;
                    })
                    // 过滤空值
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception e) {
            log.error("Elasticsearch 查询失败", e);
            throw  new ClientException("志愿者匹配服务暂时不可用，请稍后重试");
        }
    }

}




