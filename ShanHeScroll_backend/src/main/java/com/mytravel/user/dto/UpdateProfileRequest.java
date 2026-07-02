package com.mytravel.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String nickname;
    private String phone;
    private Integer gender;
    private String bio;
    private String location;
    private String avatar;
}
