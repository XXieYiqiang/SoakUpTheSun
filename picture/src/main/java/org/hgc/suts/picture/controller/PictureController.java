package org.hgc.suts.picture.controller;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.picture.common.result.Result;
import org.hgc.suts.picture.common.web.Results;
import org.hgc.suts.picture.dto.req.UploadPictureAnalysisReqDTO;
import org.hgc.suts.picture.dto.resp.UploadPictureRespDTO;
import org.hgc.suts.picture.service.PictureService;
import org.hgc.suts.picture.service.impl.PictureSpaceServiceImpl;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片管理层
 */
@RestController
@RequiredArgsConstructor
public class PictureController {

    private final PictureService pictureService;

    /**
     * 上传图片
     */
    @PostMapping("/api/picture/uploadPicture")
    public Result<UploadPictureRespDTO> uploadPicture(@RequestPart("file") MultipartFile multipartFile){
        return Results.success(pictureService.uploadPicture(multipartFile));
    }

    /**
     * 分析图片
     */
    @PostMapping("/api/picture/uploadPictureAnalysis")
    public Result<UploadPictureRespDTO> uploadPictureAnalysis(@RequestPart("file") MultipartFile multipartFile,@RequestParam String descriptionContent){
        return Results.success(pictureService.uploadPictureAnalysis(multipartFile,descriptionContent));
    }


    /**
     * 获取分析结果
     */
    @GetMapping("/api/picture/getPictureAnalysisResponse")
    public Result<String> getPictureAnalysisResponse(@RequestParam Long pictureId) {
        return Results.success(pictureService.getPictureAnalysisResponse(pictureId));
    }
}
