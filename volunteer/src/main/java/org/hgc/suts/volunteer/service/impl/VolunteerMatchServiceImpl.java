package org.hgc.suts.volunteer.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.Script;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.volunteer.common.biz.user.UserContext;
import org.hgc.suts.volunteer.common.biz.user.UserInfoDTO;
import org.hgc.suts.volunteer.common.exception.ClientException;
import org.hgc.suts.volunteer.common.manager.VolunteerRedisManager;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;
import org.hgc.suts.volunteer.dao.mapper.VolunteerUserMapper;
import org.hgc.suts.volunteer.dto.req.VolunteerMatchReqDTO;
import org.hgc.suts.volunteer.dto.resp.VolunteerMatchRespDTO;
import org.hgc.suts.volunteer.service.VolunteerMatchService;
import org.hgc.suts.volunteer.service.VolunteerUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VolunteerMatchServiceImpl implements VolunteerMatchService {

    private final VolunteerRedisManager volunteerRedisManager;
    private final VolunteerUserMapper volunteerUserMapper;

    // 返回活跃用户数量
    private static final int TARGET_MATCH_COUNT = 4;
    // 热池缺人，则返回活跃用户的倍数
    private static final int COLD_USER_MULTIPLIER = 5;
    // 活跃池中的
    private static final long ACTIVE_POOL_TTL = 30 * 60;
    private static final long COOLDOWN_SECONDS = 60;
    private final ElasticsearchClient elasticsearchClient;
    private static final String ES_INDEX_NAME = "volunteer_index";
    // 查询脚本
    private static final String MATCH_SCRIPT = ResourceUtil.readUtf8Str("scripts/volunteer_match.painless");


    @Override
    public void activateVolunteer(Long volunteerId) {
        if (volunteerId == null) throw new ClientException("志愿者ID不能为空");

        VolunteerUserDO volunteerUser = volunteerUserMapper.selectById(volunteerId);
        if (volunteerUser == null) {
            throw new ClientException("传入的志愿者ID是错误的！");
        }

        // 解析坐标
        Double[] coords = parseLocation(volunteerUser.getLocation());
        if (coords != null) {
            Double lat = coords[0];
            Double lon = coords[1];
            if (lon < -180 || lon > 180 || lat < -85.05 || lat > 85.05) {
                throw new ClientException("志愿者地理位置异常，请重新调整");
            }
            int age = Period.between(volunteerUser.getBirthday().toLocalDate(), LocalDate.now()).getYears();;
            int sex = volunteerUser.getSex() != null ? volunteerUser.getSex() : 0;

            try {
                volunteerRedisManager.addVolunteerToActivePool(volunteerId, lat, lon, age, sex, ACTIVE_POOL_TTL);
            } catch (Exception e) {
                throw new ClientException("加入热池失败(系统异常): " + e.getMessage());
            }
        } else {
            throw new ClientException("该用户坐标有误");
        }
    }


    @Override
    public List<Long> matchVolunteer(VolunteerMatchReqDTO req) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ClientException("当前用户登陆异常，稍后再试");
        }

        UserInfoDTO user = UserContext.getUser();
        if (user == null) throw new ClientException("用户不存在");

        log.info("user=={}",user);

        // 解析视障者位置
        Double[] coords = parseLocation(user.getLocation());
        if (coords == null) throw new ClientException("无法获取您的位置，请先完善信息");

        // 坐标
        Double userLat = coords[0];
        Double userLon = coords[1];

        // 计算年龄
        int userAge = Period.between(user.getBirthday().toLocalDate(), LocalDate.now()).getYears();;
        int userSex = user.getSex() != null ? user.getSex() : 0;

        List<Long> finalCandidates = new ArrayList<>();

        // 在活跃队列中匹配
        try {
            List<Long> hotIds = volunteerRedisManager.matchBestVolunteers(userLat, userLon, userAge, userSex, TARGET_MATCH_COUNT, req.getSexWeight(), req.getAgeWeight(), req.getLocationWeight());
            finalCandidates.addAll(hotIds);
        } catch (Exception e) {
            log.error("Redis热池匹配异常", e);
        }

        // 活跃队列人数没有那么多的话，使用es兜底匹配
        if (finalCandidates.size() < TARGET_MATCH_COUNT) {
            // 计算缺口,差多少人
            int missingCount = TARGET_MATCH_COUNT - finalCandidates.size();

            // 按倍数计算应该从es获得的人数(因为热池的一定是活跃的，es不一定是活跃的)
            int esSearchCount = missingCount * COLD_USER_MULTIPLIER;

            try {
                // 从es中查
                List<Long> esIds = searchFallbackFromEs(userLat, userLon, userAge, userSex, req, esSearchCount, finalCandidates
                );
                finalCandidates.addAll(esIds);
            } catch (Exception e) {
                // 即使es查询出错，那也可以把热池的人返回回去
                log.error("ES兜底查询异常", e);
            }
        }
        // 近期被匹配了就应该放进冷却池中，防止短信轰炸
        if (!finalCandidates.isEmpty()) {
            volunteerRedisManager.setCooldown(finalCandidates, COOLDOWN_SECONDS);
        }

        return finalCandidates;
    }


    // 获取坐标 纬度，经度
    private Double[] parseLocation(String locStr) {
        if (StrUtil.isBlank(locStr)) return null;
        String[] parts = locStr.replace("，", ",").split(",");
        if (parts.length == 2) {
            try {
                double lat = Double.parseDouble(parts[0].trim());
                double lon = Double.parseDouble(parts[1].trim());
                return new Double[]{lat, lon};
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private List<Long> searchFallbackFromEs(Double userLat, Double userLon, int userAge, int userSex, VolunteerMatchReqDTO requestParm, int targetCount, List<Long> excludeIds) {
        // 1. 构建参数
        Map<String, JsonData> params = new HashMap<>();
        params.put("sexWeight", JsonData.of(Math.abs(requestParm.getSexWeight())));
        params.put("ageWeight", JsonData.of(Math.abs(requestParm.getAgeWeight())));
        params.put("locationWeight", JsonData.of(Math.abs(requestParm.getLocationWeight()) * 0.00001));

        params.put("userSex", JsonData.of(userSex));
        params.put("userAge", JsonData.of(userAge));
        params.put("userLon", JsonData.of(userLon));
        params.put("userLat", JsonData.of(userLat));
        params.put("currentYear", JsonData.of(LocalDate.now().getYear()));

        try {
            Script rankingScript = new Script.Builder()
                    .inline(in -> in.source(MATCH_SCRIPT).params(params))
                    .build();

            String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            // 2. 构建查询 Query
            Query activeDataQuery = new Query.Builder().bool(b -> {
                // 过滤
                b.must(m -> m.term(t -> t.field("delFlag").value(0)));
//                b.must(m -> m.range(r -> r.field("startTime").lte(JsonData.of(nowTime))));
//                b.must(m -> m.range(r -> r.field("endTime").gte(JsonData.of(nowTime))));

                // 地理位置初筛
                b.filter(f -> f.geoDistance(g -> g
                        .field("location")
                        .distance("1000km")
                        .location(l -> l.latlon(ll -> ll.lat(userLat).lon(userLon)))
                ));

                // 排除 Redis 已经选中的 ID
                if (CollUtil.isNotEmpty(excludeIds)) {
                    List<FieldValue> terms = excludeIds.stream()
                            .map(FieldValue::of)
                            .collect(Collectors.toList());
                    b.mustNot(mn -> mn.terms(t -> t.field("id").terms(v -> v.value(terms))));
                }
                return b;
            }).build();

            // 3. 执行搜索,获取id
            SearchResponse<Object> response = elasticsearchClient.search(s -> s
                            .index(ES_INDEX_NAME)
                            // 获取数量
                            .size(targetCount)
                            // 筛选id
                            .source(src -> src.fetch(false))
                            .query(q -> q
                                    .functionScore(fs -> fs
                                            .query(activeDataQuery)
                                            .functions(f -> f.scriptScore(ss -> ss.script(rankingScript)))
                                            .boostMode(FunctionBoostMode.Replace)
                                    )
                            ),
                    Object.class
            );

            // 4. 提取 ID
            if (response.hits().hits().isEmpty()) {
                return new ArrayList<>();
            }
            return response.hits().hits().stream()
                    .map(hit -> Long.parseLong(hit.id()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("ES兜底匹配失败", e);
            return new ArrayList<>();
        }
    }
}