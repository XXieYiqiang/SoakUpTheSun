package org.hgc.suts.picture.common.tensentCos;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hgc.suts.picture.common.exception.ClientException;
import org.hgc.suts.picture.common.exception.ServiceException;
import org.hgc.suts.picture.config.CosClientConfig;
import org.hgc.suts.picture.dto.resp.UploadPictureCosRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilePictureUpload {

    private final CosClientConfig cosClientConfig;
    private final CosManager cosManager;

    public UploadPictureCosRespDTO uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(multipartFile);

        // 2. 生成路径
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);

        // 3. 本地计算文件数据
        int picWidth = 0;
        int picHeight = 0;
        try (InputStream imageStream = multipartFile.getInputStream()) {
            BufferedImage bufferedImage = ImageIO.read(imageStream);
            if (bufferedImage != null) {
                picWidth = bufferedImage.getWidth();
                picHeight = bufferedImage.getHeight();
            }
        } catch (Exception e) {
            log.warn("图片宽高解析失败，将存为0", e);
        }

        // 4. 执行上传
        try (InputStream inputStream = multipartFile.getInputStream()) {
            long fileLength = multipartFile.getSize();
            // 上传原图
            cosManager.putPictureObject(uploadPath, inputStream, fileLength);

            return buildResult(originalFilename, uploadPath, fileLength, picWidth, picHeight);

        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new ServiceException("图片上传失败");
        }
    }

    /**
     * 构造返回结果
     */
    private UploadPictureCosRespDTO buildResult(String originalFilename, String uploadPath, long fileLength, int picWidth, int picHeight) {
        double picScale = 0.0;
        if (picHeight != 0) {
            picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        }

        String host = cosClientConfig.getHost();

        // 直接返回缩略图地址,cos会自动处理
        String thumbnailUrl = host + "/" + uploadPath + "?imageMogr2/thumbnail/256x256";

        return UploadPictureCosRespDTO.builder()
                .url(host + "/" + uploadPath)
                .thumbnailUrl(thumbnailUrl)
                .picName(FileUtil.mainName(originalFilename))
                .picSize(fileLength)
                .picWidth(picWidth)
                .picHeight(picHeight)
                .picScale(picScale)
                .picFormat(FileUtil.getSuffix(originalFilename))
                .build();
    }

    private void validPicture(MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new ClientException("图片不能为空");
        }
        long fileSize = multipartFile.getSize();
        if (fileSize > 2 * 1024 * 1024) {
            throw new ClientException("图片超过2MB");
        }
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        if (!ALLOW_FORMAT_LIST.contains(fileSuffix)) {
            throw new ClientException("图片格式错误");
        }
    }
}