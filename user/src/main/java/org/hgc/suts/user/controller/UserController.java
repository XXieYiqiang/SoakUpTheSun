package org.hgc.suts.user.controller;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.user.common.result.Result;
import org.hgc.suts.user.common.web.Results;
import org.hgc.suts.user.dto.req.UserLoginReqDTO;
import org.hgc.suts.user.dto.req.UserRegisterReqDTO;
import org.hgc.suts.user.dto.resp.UserLoginRespDTO;
import org.hgc.suts.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @PostMapping("/api/user/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/api/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }
}
