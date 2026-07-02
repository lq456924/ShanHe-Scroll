package com.mytravel.user.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer gender;
    private String bio;
    private String location;
    private Integer role;
    private Integer status;
}
