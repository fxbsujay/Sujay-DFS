package com.susu.dfs.common;

import lombok.Data;

import java.util.ArrayList;

/**
 * <p>Description: Tracker 的用户信息</p>
 *
 * @author sujay
 * @version 10:14 2022/9/19
 */
@Data
public class User {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 秘钥
     */
    private String secret;

    /**
     * 创建时间
     */
    private long createTime;


    public static User copy(User user) {
        User ret = new User();
        ret.setUsername(user.username);
        ret.setSecret(user.secret);
        return ret;
    }
}
