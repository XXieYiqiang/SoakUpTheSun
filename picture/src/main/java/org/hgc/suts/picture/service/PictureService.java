package org.hgc.suts.picture.service;

import org.hgc.suts.picture.dao.entity.PictureDO;
import com.baomidou.mybatisplus.extension.service.IService;
import org.hgc.suts.picture.dto.resp.UploadPictureRespDTO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 谢毅强
* @description 针对表【t_picture(图片)】的数据库操作Service
* @createDate 2025-12-16 14:38:07
*/
public interface PictureService extends IService<PictureDO> {

    /**
     * 上传图片
     * @param multipartFile 图片
     * @return 上传图片信息
     */
    UploadPictureRespDTO uploadPicture(MultipartFile multipartFile);
}
