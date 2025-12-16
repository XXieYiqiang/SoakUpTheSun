package org.hgc.suts.picture.controller;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.picture.common.result.Result;
import org.hgc.suts.picture.common.web.Results;
import org.hgc.suts.picture.service.PictureService;
import org.hgc.suts.picture.service.impl.PictureSpaceServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
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
    public Result<Void> uploadPicture(@RequestPart("file") MultipartFile multipartFile){
        pictureService.uploadPicture(multipartFile);
        return Results.success();
    };
}
