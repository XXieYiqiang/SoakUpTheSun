package org.hgc.suts.user.controller;


import lombok.RequiredArgsConstructor;
import org.hgc.suts.user.common.result.Result;
import org.hgc.suts.user.common.web.Results;
import org.hgc.suts.user.dto.req.UserLoginReqDTO;
import org.hgc.suts.user.dto.req.UserRegisterReqDTO;
import org.hgc.suts.user.dto.resp.UserLoginRespDTO;
import org.hgc.suts.user.dto.resp.UserRespDTO;
import org.hgc.suts.user.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/api/user/register")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam) {
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 用户登陆
     */
    @PostMapping("/api/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userService.login(requestParam));
    }


    /**
     * 根据用户名查询用户信息
     */
    @GetMapping("/api/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }

}
