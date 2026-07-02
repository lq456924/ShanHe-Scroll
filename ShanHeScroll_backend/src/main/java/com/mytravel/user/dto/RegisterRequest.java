package com.mytravel.user.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    /** 邮箱（必填，作为登录账号） */
    private String email;
    /** 密码（必填） */
    private String password;
    /** 昵称（必填，2-20位，仅允许英文字母、数字、下划线、连字符、点号） */
    private String nickname;
    /** 邮箱验证码（必填，6位数字） */
    private String code;
}
