package com.mytravel.user.dto;

import lombok.Data;

@Data
public class LoginRequest {
    /** 用户名或邮箱（必填） */
    private String account;
    /** 密码（必填） */
    private String password;
}
