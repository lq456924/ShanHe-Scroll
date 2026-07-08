package com.mytravel.user.service;
import com.mytravel.user.User;
import com.mytravel.user.repository.UserRepository;
import com.mytravel.user.dto.UpdateProfileRequest;
import com.mytravel.user.dto.UserDto;
import com.mytravel.user.dto.BindPhoneRequest;

import com.mytravel.user.User;
import com.mytravel.user.dto.BindPhoneRequest;
import com.mytravel.user.dto.UpdateProfileRequest;
import com.mytravel.user.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    public UserDto getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return UserMapper.toDto(user);
    }

    public UserDto updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (request.getNickname() != null) user.setNickname(request.getNickname());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getLocation() != null) user.setLocation(request.getLocation());
        if (request.getAvatar() != null) user.setAvatar(request.getAvatar());

        userRepository.save(user);
        return UserMapper.toDto(user);
    }

    public void bindPhone(String username, BindPhoneRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPhone(request.getPhone());
        userRepository.save(user);
    }
}
