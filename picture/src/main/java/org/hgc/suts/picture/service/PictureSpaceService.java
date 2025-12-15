package org.hgc.suts.picture.service;

import org.hgc.suts.picture.dao.entity.PictureSpace;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author 谢毅强
* @description 针对表【t_picture_space(空间)】的数据库操作Service
* @createDate 2025-12-15 15:09:51
*/
public interface PictureSpaceService extends IService<PictureSpace> {
    void createPictureSpace(Long userId);
}
