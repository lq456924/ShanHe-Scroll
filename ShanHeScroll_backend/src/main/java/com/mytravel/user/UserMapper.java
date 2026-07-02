package com.mytravel.user;

import com.mytravel.user.dto.UserDto;

/**
 * Entity ↔ DTO 转换，供 AuthService、UserProfileService 共用。
 */
final class UserMapper {

    private UserMapper() {}

    static UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setBio(user.getBio());
        dto.setLocation(user.getLocation());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
