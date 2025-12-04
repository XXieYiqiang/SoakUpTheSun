package org.hgc.suts.volunteer.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.hgc.suts.volunteer.dao.entity.VolunteerRatingDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author 谢毅强
* @description 针对表【volunteer_rating】的数据库操作Mapper
* @createDate 2025-12-04 19:25:59
* @Entity generator.domain.VolunteerRating
*/
public interface VolunteerRatingMapper extends BaseMapper<VolunteerRatingDO> {

    /**
     * 规则：
     * 1. rating = 1 (普通＋附加): +1.0 分
     * 2. rating = 0 (普通): +0.7 分
     */
    @Select("SELECT user_id as userId, " +
            // 【核心修改点】 SQL 逻辑：如果是 1 则加 1.0，否则加 0.7
            "SUM(CASE WHEN rating = 1 THEN 1.0 ELSE 0.7 END) as totalAddScore, " +
            "GROUP_CONCAT(id) as idListStr " +
            "FROM volunteer_rating " +
            "WHERE is_calculated = 0 " +
            "GROUP BY user_id")
    List<Map<String, Object>> selectAggregatedUncalculatedRatings();

    /**
     * 批量更新状态
     */
    @Update("<script>" +
            "UPDATE volunteer_rating SET is_calculated = 1, update_time = NOW() " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    void batchUpdateCalculatedStatus(@Param("ids") List<Long> ids);
}





