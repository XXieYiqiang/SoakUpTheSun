package org.hgc.suts.volunteer.dao.mapper;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.mapping.ResultSetType;
import org.hgc.suts.volunteer.dao.entity.VolunteerUserDO;

/**
* @author 谢毅强
* @description 针对表【volunteer_user】的数据库操作Mapper
* @createDate 2025-12-01 16:20:43
* @Entity generator.domain.VolunteerUser
*/
public interface VolunteerUserMapper extends BaseMapper<VolunteerUserDO> {
    /**
     * 新增志愿分
     * @param id volunteerId
     * @param addScore 要增加的分数
     */
    @Update("UPDATE volunteer_user SET score = IFNULL(score, 0) + #{addScore} WHERE id = #{id}")
    void addScore(@Param("id") Long id, @Param("addScore") Double addScore);


    /**
     * 1. 查询第 N 名的分数是多少
     */
    @Select("SELECT score FROM volunteer_user WHERE del_flag = 0 ORDER BY score DESC LIMIT #{offset}, 1")
    Double selectScoreByRank(@Param("offset") long offset);

    /**
     * 2. 流式查询所有获奖用户 (分数 >= 分数线)
     */
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    @Select("SELECT * FROM volunteer_user WHERE score >= #{minScore} AND del_flag = 0")
    Cursor<VolunteerUserDO> scanWinners(@Param("minScore") Double minScore);

    /**
     * 查询前N名获奖用户
     * @param limit N名
     * @return 获奖用户
     */
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = Integer.MIN_VALUE)
    @Select("SELECT * FROM volunteer_user WHERE del_flag = 0 ORDER BY score DESC, id ASC LIMIT #{limit}")
    Cursor<VolunteerUserDO> scanTopNUsers(@Param("limit") long limit);
}




