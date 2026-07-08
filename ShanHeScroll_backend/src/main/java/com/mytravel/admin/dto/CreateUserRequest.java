package com.mytravel.admin.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String password;
    private String nickname;
    private String email;
    /** 角色: 0=普通用户  1=审核员  2=管理员 */
    private Integer role;
}
