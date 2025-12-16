package org.hgc.suts.picture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.hgc.suts.picture.constant.defaultSpaceConstant;
import org.hgc.suts.picture.dao.entity.PictureSpaceDO;
import org.hgc.suts.picture.service.PictureSpaceService;
import org.hgc.suts.picture.dao.mapper.PictureSpaceMapper;
import org.springframework.stereotype.Service;

/**
* @author 谢毅强
* @description 针对表【t_picture_space(空间)】的数据库操作Service实现
* @createDate 2025-12-15 15:09:51
*/
@Service
public class PictureSpaceServiceImpl extends ServiceImpl<PictureSpaceMapper, PictureSpaceDO> implements PictureSpaceService{

    @Override
    public void createPictureSpace(Long userId) {
        PictureSpaceDO pictureSpaceDO = PictureSpaceDO.builder()
                .spaceName(defaultSpaceConstant.spaceName)
                .userId(userId)
                .maxSize(defaultSpaceConstant.maxSize)
                .maxCount(defaultSpaceConstant.maxCount)
                .build();
        try {
            baseMapper.insert(pictureSpaceDO);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}




