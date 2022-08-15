package com.susu.dfs.tracker.tomcat.controller;

import com.susu.dfs.common.Result;
import com.susu.dfs.tracker.tomcat.annotation.RequestMapping;
import com.susu.dfs.tracker.tomcat.annotation.RestController;
import com.susu.dfs.tracker.tomcat.dto.LoginDTO;

/**
 * <p>Description: 登录认证</p>
 *
 * @author sujay
 * @version 17:12 2022/8/15
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ADMIN = "admin";

    @RequestMapping("/login")
    public Result<Boolean> login(LoginDTO dto) {
        if (ADMIN.equalsIgnoreCase(dto.getUsername()) && ADMIN.equalsIgnoreCase(dto.getPassword())) {
            return Result.ok(true);
        }
        return Result.error("密码错误");
    }

}
