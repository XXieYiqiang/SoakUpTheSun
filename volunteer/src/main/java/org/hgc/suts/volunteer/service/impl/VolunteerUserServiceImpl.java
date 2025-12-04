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
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReq;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchResp;
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


    private final ElasticsearchClient elasticsearchClient;
    private static final String ES_INDEX_NAME = "volunteer_index";
    // 查询脚本
    private static final String MATCH_SCRIPT = ResourceUtil.readUtf8Str("scripts/volunteer_match.painless");



    @Override
    public List<VolunteerMatchResp> matchVolunteer(VolunteerMatchReq requestParam) {

        // 1. 当前用户的数据参数
        // 计算当前用户的年龄
        int userAge = Period.between(Objects.requireNonNull(UserContext.getBirthday()).toLocalDate(), LocalDate.now()).getYears();

        // 解析当前用户的经纬度 (假设 location 格式为 "lon,lat")
        String[] locationParts = Objects.requireNonNull(UserContext.getLocation()).split(",");
        double userLon = Double.parseDouble(locationParts[0]);
        double userLat = Double.parseDouble(locationParts[1]);

        // 2. 构建 评分 脚本参数 map
        Map<String, JsonData> params = new HashMap<>();
        
        params.put("sexWeight", JsonData.of(Math.abs(requestParam.getSexWeight()))); // 性别权重
        params.put("ageWeight", JsonData.of(Math.abs(requestParam.getAgeWeight()))); // 年龄权重
        params.put("locationWeight", JsonData.of(Math.abs(requestParam.getLocationWeight())*0.00001)); // 距离权重

        params.put("userSex", JsonData.of(UserContext.getUserSex()));
        params.put("userAge", JsonData.of(userAge));
        params.put("userLon", JsonData.of(userLon));
        params.put("userLat", JsonData.of(userLat));

        // 今年是几年
        params.put("currentYear", JsonData.of(LocalDate.now().getYear()));

        try {

            Script rankingScript = new Script.Builder()
                    .inline(in -> in
                            .source(MATCH_SCRIPT)
                            .params(params)
                    )
                    .build();

            // 记录当前时间
            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            //设置过滤条件
            Query activeDataQuery = new Query.Builder()
                    .bool(b -> b
                            // 条件1：未删除
                            .must(m -> m.term(t -> t.field("delFlag").value(0)))
                            // 条件2：startTime <= nowTime
                            .must(m -> m.range(r -> r.field("startTime").lte(JsonData.of(nowTime))))
                            // 条件3：endTime >= nowTime
                            .must(m -> m.range(r -> r.field("endTime").gte(JsonData.of(nowTime))))
                            // 条件4:增加地理位置过滤器,减少运行脚本的数量
                            .filter(f -> f.geoDistance(g -> g.field("location").distance("1000km")
                                            .location(l -> l.latlon(ll -> ll
                                                            .lat(userLat)
                                                            .lon(userLon)))))
                    )
                    .build();

            // 3. 执行查询 (只获取 ID)
            SearchResponse<VolunteerUserDO> response = elasticsearchClient.search(s -> s
                            .index(ES_INDEX_NAME)
                            .size(50)
                            // 不查 _source 详情
                            .source(src -> src.fetch(false))
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            .query(activeDataQuery)
                                            .functions(f -> f.scriptScore(ss -> ss.script(rankingScript)))
                                            .boostMode(FunctionBoostMode.Replace)
                                    )
                            ),
                    VolunteerUserDO.class
            );
            // 如果 ES 没结果，直接返回空
            if (response.hits().hits().isEmpty()) {
                return new ArrayList<>();
            }

            // 4. 提取 ID 和 匹配度分数
            List<Long> idList = new ArrayList<>();
            Map<Long, Double> scoreMap = new HashMap<>(); // 用于暂存 ID -> 匹配度

            response.hits().hits().forEach(hit -> {
                Long id = Long.parseLong(hit.id());
                idList.add(id);
                scoreMap.put(id, hit.score()); // 保存 ES 算出来的匹配度
            });

            // 5. MySQL 回表查询 获取最新消息
            List<VolunteerUserDO> dbUserList = this.listByIds(idList);

            // 6. 数据组装与重排序
            return dbUserList.stream()
                    .map(userDO -> {
                        VolunteerMatchResp resp = BeanUtil.toBean(userDO, VolunteerMatchResp.class);
                        // 从 Map 中把 ES 的匹配度填回去
                        Double score = scoreMap.get(userDO.getId());
                        if (score != null) {
                            resp.setMatchDegree(score);
                        }
                        return resp;
                    })
                    // 因为 MySQL listByIds 返回的顺序可能和 idList 不一致，必须重新按分数降序排
                    .sorted(Comparator.comparing(VolunteerMatchResp::getMatchDegree).reversed())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Elasticsearch 查询失败", e);
            throw  new ClientException("志愿者匹配服务暂时不可用，请稍后重试");
        }
    }

}




