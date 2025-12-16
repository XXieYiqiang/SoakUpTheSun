package org.hgc.suts.picture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.hgc.suts.picture.common.biz.user.UserContext;
import org.hgc.suts.picture.common.biz.user.UserInfoDTO;
import org.hgc.suts.picture.common.constant.RedisCacheConstant;
import org.hgc.suts.picture.common.exception.ClientException;
import org.hgc.suts.picture.common.tensentCos.FilePictureUpload;
import org.hgc.suts.picture.dao.entity.PictureDO;
import org.hgc.suts.picture.dao.entity.PictureSpaceDO;
import org.hgc.suts.picture.dao.mapper.PictureSpaceMapper;
import org.hgc.suts.picture.dto.resp.UploadPictureCosRespDTO;
import org.hgc.suts.picture.dto.resp.UploadPictureRespDTO;
import org.hgc.suts.picture.mq.event.UploadPictureAnalysisEvent;
import org.hgc.suts.picture.mq.producer.PictureAnalysisSendProducer;
import org.hgc.suts.picture.service.PictureService;
import org.hgc.suts.picture.dao.mapper.PictureMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
* @author 谢毅强
* @description 针对表【t_picture(图片)】的数据库操作Service实现
* @createDate 2025-12-16 14:38:07
*/
@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, PictureDO> implements PictureService {

    private final PictureSpaceMapper pictureSpaceMapper;
    private final FilePictureUpload filePictureUpload;
    private final TransactionTemplate transactionTemplate;
    private final PictureAnalysisSendProducer pictureAnalysisSendProducer;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public String getPictureAnalysisResponse(Long pictureId) {
        LambdaQueryWrapper<PictureDO> queryWrapper = Wrappers.lambdaQuery(PictureDO.class)
                .eq(PictureDO::getUserId, UserContext.getUserId())
                .eq(PictureDO::getId, pictureId);
        PictureDO pictureDO = this.getOne(queryWrapper);
        if (pictureDO == null) {
            throw new ClientException("图片非该用户上传");
        }
        String pictureAnalysisKey = String.format(RedisCacheConstant.PICTURE_ANALYSIS_RESPONSE_KEY, pictureId);
        return stringRedisTemplate.opsForValue().get(pictureAnalysisKey);
    }

    @Override
    public UploadPictureRespDTO uploadPictureAnalysis(MultipartFile multipartFile) {

        if (multipartFile == null) {
            throw new ClientException("图片是空的，无法分析");
        }

        UploadPictureRespDTO uploadPictureRespDTO = this.uploadPicture(multipartFile);
        UploadPictureAnalysisEvent uploadPictureAnalysisEvent = UploadPictureAnalysisEvent.builder()
                .pictureId(uploadPictureRespDTO.getId())
                .imageKey(uploadPictureRespDTO.getUrl())
                .build();
        pictureAnalysisSendProducer.sendMessage(uploadPictureAnalysisEvent);
        return uploadPictureRespDTO;
    }


    @Override
    public UploadPictureRespDTO uploadPicture(MultipartFile multipartFile) {
        UserInfoDTO user = UserContext.getUser();
        if (user == null) {
            throw new ClientException("非法操作，请重新登陆");
        }
        if (multipartFile.isEmpty()) {
            throw new ClientException("文件为空，请重试");
        }

        LambdaQueryWrapper<PictureSpaceDO> queryWrapper = Wrappers.lambdaQuery(PictureSpaceDO.class).eq(PictureSpaceDO::getUserId, user.getId());
        PictureSpaceDO pictureSpaceDO = pictureSpaceMapper.selectOne(queryWrapper);
        if (pictureSpaceDO == null) {
            throw new ClientException("用户未初始化空间，请联系工作人员");
        }

        // 预判空间是否足够，避免浪费 COS 上传流量
         if (pictureSpaceDO.getTotalSize() + multipartFile.getSize() > pictureSpaceDO.getMaxSize()) {
             throw new ClientException("空间容量不足");
         }

        // 上传图片，得到图片信息
        String uploadPathPrefix;
        if (pictureSpaceDO.getId() == null) {
            uploadPathPrefix = String.format("public/%s", user.getId());
        } else {
            uploadPathPrefix = String.format("space/%s", pictureSpaceDO.getId());
        }

        UploadPictureCosRespDTO uploadPictureCosRespDTO = filePictureUpload.uploadPicture(multipartFile, uploadPathPrefix);

        // 构造要入库的图片信息

        PictureDO picture = PictureDO.builder()
                .spaceId(pictureSpaceDO.getId())
                .url(uploadPictureCosRespDTO.getUrl())
                .thumbnailUrl(uploadPictureCosRespDTO.getThumbnailUrl())
                .picSize(uploadPictureCosRespDTO.getPicSize())
                .picWidth(uploadPictureCosRespDTO.getPicWidth())
                .picHeight(uploadPictureCosRespDTO.getPicHeight())
                .picScale(uploadPictureCosRespDTO.getPicScale())
                .picFormat(uploadPictureCosRespDTO.getPicFormat())
                .userId(user.getId())
                .build();

        // 假如没有返回错误，那就让图片名字为当时时间
        String picName = uploadPictureCosRespDTO.getPicName();
        if (StrUtil.isNotBlank(picName)) {
            picture.setName(picName);
        } else {
            picture.setName(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        }

        // 6. 开启事务
        transactionTemplate.execute(status -> {
            // 6.1 保存图片
            boolean result = this.save(picture);
            if (!result) {
                throw new ClientException("上传图片错误，请稍后再试");
            }

            // 6.2 更新空间额度
            Long spaceId = pictureSpaceDO.getId();
            if (spaceId != null) {
                LambdaUpdateWrapper<PictureSpaceDO> updateWrapper = Wrappers.lambdaUpdate(PictureSpaceDO.class)
                        .eq(PictureSpaceDO::getId, spaceId)
                        .setSql("total_size = total_size + " + picture.getPicSize())
                        .setSql("total_count = total_count + 1");
                int update = pictureSpaceMapper.update(updateWrapper);
                if (update < 0) {
                    throw new ClientException("额度修改失败,稍后再试");
                }
            }
            return picture;
        });

        return BeanUtil.toBean(picture,UploadPictureRespDTO.class);
    }

}




