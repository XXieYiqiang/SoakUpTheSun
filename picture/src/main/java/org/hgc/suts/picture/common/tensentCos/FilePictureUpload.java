package org.hgc.suts.picture.common.tensentCos;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.picture.common.exception.ClientException;
import org.hgc.suts.picture.common.exception.ServiceException;
import org.hgc.suts.picture.config.CosClientConfig;
import org.hgc.suts.picture.dto.resp.UploadPictureCosRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件图片上传服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FilePictureUpload {


    private final CosClientConfig cosClientConfig;


    private final CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile    前端传来的文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 封装好的返回结果
     */
    public UploadPictureCosRespDTO uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(multipartFile);

        // 2. 生成图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        // 流式上传
        try (InputStream inputStream = multipartFile.getInputStream()) {
            // 获取文件大小
            long fileLength = multipartFile.getSize();

            // 3. 上传图片到对象存储 (直接传流)
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, inputStream, fileLength);

            // 4. 获取图片信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();

            // 如果有压缩处理结果
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取压缩之后得到的文件信息
                CIObject compressedCiObject = objectList.get(0);
                // 缩略图默认等于压缩图
                CIObject thumbnailCiObject = compressedCiObject;
                // 有生成缩略图，才获取缩略图 (index 1 通常是缩略图，根据 CosManager 里的 rule 添加顺序)
                if (objectList.size() > 1) {
                    thumbnailCiObject = objectList.get(1);
                }
                // 封装压缩图的返回结果
                return buildResult(originalFilename, compressedCiObject, thumbnailCiObject, imageInfo);
            }

            // 如果没有压缩处理（虽然你的逻辑里肯定有，但为了健壮性保留兜底）
            return buildResult(originalFilename, uploadPath, fileLength, imageInfo);

        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new ServiceException("图片上传失败");
        }
    }

    /**
     * 校验文件
     */
    private void validPicture(MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new ClientException("图片不能为空");
        }
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        if (fileSize > 2 * ONE_M){
            throw new ClientException("图片超过2MB");
        }
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        if (!ALLOW_FORMAT_LIST.contains(fileSuffix)) {
            throw new ClientException("图片格式错误");
        }
    }

    /**
     * 封装返回结果（有压缩/缩略图情况）
     */
    private UploadPictureCosRespDTO buildResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject,
                                                ImageInfo imageInfo) {
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        // 设置压缩后的原图地址
        UploadPictureCosRespDTO uploadPictureCosRespDTO = UploadPictureCosRespDTO.builder()
                .url(cosClientConfig.getHost() + "/" + compressedCiObject.getKey())
                .picName(FileUtil.mainName(originalFilename))
                .picSize(compressedCiObject.getSize().longValue())
                .picWidth(picWidth)
                .picHeight(picHeight)
                .picScale(picScale)
                .picFormat(compressedCiObject.getFormat())
                .build();

        // 设置缩略图地址
        uploadPictureCosRespDTO.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        
        return uploadPictureCosRespDTO;
    }

    /**
     * 封装返回结果（无压缩/兜底情况）
     */
    private UploadPictureCosRespDTO buildResult(String originalFilename, String uploadPath, long fileLength, ImageInfo imageInfo) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();

        return UploadPictureCosRespDTO.builder()
                .url(cosClientConfig.getHost() + "/" + uploadPath)
                .picName(FileUtil.mainName(originalFilename))
                .picSize(fileLength)
                .picWidth(picWidth)
                .picHeight(picHeight)
                .picScale(picScale)
                .picFormat(imageInfo.getFormat())
                .build();
    }
}