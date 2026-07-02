package com.mytravel.user.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserDto user;
}
