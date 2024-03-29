package com.susu.dfs.tracker.tomcat.controller;

import com.susu.dfs.common.Result;
import com.susu.dfs.tracker.tomcat.annotation.RequestBody;
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

    @RequestMapping( value = "/login", method = "POST")
    public Result<LoginDTO> login(@RequestBody LoginDTO dto) {
        if (ADMIN.equalsIgnoreCase(dto.getUsername()) && ADMIN.equalsIgnoreCase(dto.getPassword())) {
            LoginDTO loginDTO = new LoginDTO();
            loginDTO.setToken("123456");
            return Result.ok(loginDTO);
        }
        return Result.error("密码错误");
    }

}
