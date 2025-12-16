package org.hgc.suts.picture.controller;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.picture.common.result.Result;
import org.hgc.suts.picture.common.web.Results;
import org.hgc.suts.picture.service.impl.PictureSpaceServiceImpl;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图片空间管理层
 */
@RestController
@RequiredArgsConstructor
public class PictureSpaceController {
    private final PictureSpaceServiceImpl pictureSpaceService;

    /**
     * 创建空间
     * @param UserId userid
     * @return 创建成功
     */
    @PostMapping("/api/picture/createPictureSpace")
    public Result<Void> createPictureSpace(@RequestParam Long UserId){
        pictureSpaceService.createPictureSpace(UserId);
        return Results.success();
    };
}
