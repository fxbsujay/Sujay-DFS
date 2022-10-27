package com.susu.dfs.common.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
@Data
public class AuthConfig {

    private String username;

    private String password;

    private String token;

    private Date authTime;

}
