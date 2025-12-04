package org.hgc.suts.volunteer.dao.mapper;

import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Update;
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
}




