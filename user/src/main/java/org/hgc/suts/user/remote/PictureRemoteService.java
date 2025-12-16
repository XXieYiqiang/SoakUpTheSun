package org.hgc.suts.user.remote;


import org.hgc.suts.user.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "picture")
public interface PictureRemoteService {
    @PostMapping("/api/picture/createPictureSpace")
    public Result<Void> createPictureSpace(@RequestParam Long UserId);
}
