package com.mytravel.admin.controller;

import com.mytravel.bottle.DriftBottle;
import com.mytravel.bottle.repository.DriftBottleRepository;
import com.mytravel.bottle.service.DriftBottleService;
import com.mytravel.common.ApiResponse;
import com.mytravel.message.MessageService;
import com.mytravel.message.MessageRepository;
import com.mytravel.travel.Attraction;
import com.mytravel.travel.repository.AttractionRepository;
import com.mytravel.travel.service.TravelService;
import com.mytravel.admin.dto.CreateUserRequest;
import com.mytravel.admin.dto.UpdateRoleRequest;
import com.mytravel.user.User;
import com.mytravel.user.dto.UserDto;
import com.mytravel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DriftBottleService bottleService;
    private final DriftBottleRepository bottleRepository;
    private final TravelService travelService;
    private final UserRepository userRepository;
    private final AttractionRepository attractionRepository;
    private final MessageService messageService;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== 景点审核（审核员及以上） ====================

    @GetMapping("/attractions/pending")
    public ApiResponse<List<Attraction>> pendingAttractions(Authentication authentication) {
        requireReviewer(authentication);
        return ApiResponse.ok(travelService.getPendingAttractions());
    }

    @PutMapping("/attractions/{id}/approve")
    public ApiResponse<String> approveAttraction(@PathVariable Long id,
                                                  Authentication authentication) {
        requireReviewer(authentication);
        travelService.approveAttraction(id);
        Attraction attr = attractionRepository.findById(id).orElse(null);
        if (attr != null && attr.getSubmitterId() != null) {
            messageService.send(attr.getSubmitterId(), "ATTRACTION_APPROVED",
                    "打卡点「" + attr.getName() + "」审核通过",
                    "您提交的打卡点已通过审核，现在对其他用户可见。",
                    id);
        }
        return ApiResponse.ok("审核通过");
    }

    @PutMapping("/attractions/{id}/reject")
    public ApiResponse<String> rejectAttraction(@PathVariable Long id,
                                                 Authentication authentication) {
        requireReviewer(authentication);
        travelService.rejectAttraction(id);
        Attraction attr = attractionRepository.findById(id).orElse(null);
        if (attr != null && attr.getSubmitterId() != null) {
            messageService.send(attr.getSubmitterId(), "ATTRACTION_REJECTED",
                    "打卡点「" + attr.getName() + "」审核未通过",
                    "您提交的打卡点未通过审核，请修改后重新提交。",
                    id);
        }
        return ApiResponse.ok("已拒绝");
    }

    // ==================== 漂流瓶审核（审核员及以上） ====================

    @GetMapping("/bottles/pending")
    public ApiResponse<List<DriftBottle>> pendingList(Authentication authentication) {
        requireReviewer(authentication);
        return ApiResponse.ok(bottleService.getPendingBottles());
    }

    @PutMapping("/bottles/{id}/approve")
    public ApiResponse<String> approve(@PathVariable Long id,
                                       Authentication authentication) {
        requireReviewer(authentication);
        bottleService.approveBottle(id);
        DriftBottle bottle = bottleRepository.findById(id).orElse(null);
        if (bottle != null && bottle.getSenderId() != null) {
            boolean isReReview = messageRepository.existsByRelatedIdAndType(id, "BOTTLE_REPORTED");
            String type = isReReview ? "BOTTLE_RE_APPROVED" : "BOTTLE_APPROVED";
            String title = isReReview ? "漂流瓶重新审核通过" : "漂流瓶审核通过";
            String content = isReReview
                    ? "您的漂流瓶在被举报后经重新审核已通过。"
                    : "您发布的漂流瓶已通过审核，现在可被其他用户拾取。";
            messageService.send(bottle.getSenderId(), type, title, content, id);
        }
        return ApiResponse.ok("审核通过");
    }

    @PutMapping("/bottles/{id}/reject")
    public ApiResponse<String> reject(@PathVariable Long id,
                                      Authentication authentication) {
        requireReviewer(authentication);
        bottleService.rejectBottle(id);
        DriftBottle bottle = bottleRepository.findById(id).orElse(null);
        if (bottle != null && bottle.getSenderId() != null) {
            boolean isReReview = messageRepository.existsByRelatedIdAndType(id, "BOTTLE_REPORTED");
            String type = isReReview ? "BOTTLE_RE_REJECTED" : "BOTTLE_REJECTED";
            String title = isReReview ? "漂流瓶重新审核未通过" : "漂流瓶审核未通过";
            String content = isReReview
                    ? "您的漂流瓶在被举报后经重新审核仍未通过。"
                    : "您发布的漂流瓶未通过审核，请检查内容后重新发布。";
            messageService.send(bottle.getSenderId(), type, title, content, id);
        }
        return ApiResponse.ok("已拒绝");
    }

    // ==================== 用户管理（仅管理员） ====================

    @GetMapping("/users")
    public ApiResponse<List<UserDto>> userList(Authentication authentication) {
        requireAdmin(authentication);
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserDto> dtos = users.stream().map(this::toDto).toList();
        return ApiResponse.ok(dtos);
    }

    @PostMapping("/users")
    public ApiResponse<UserDto> createUser(@RequestBody CreateUserRequest request,
                                           Authentication authentication) {
        requireAdmin(authentication);

        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("密码不能为空");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : 0);
        user.setStatus(0);

        user = userRepository.save(user);
        return ApiResponse.ok(toDto(user));
    }

    @PutMapping("/users/{id}/reset-password")
    public ApiResponse<String> resetPassword(@PathVariable Long id,
                                             @RequestBody CreateUserRequest request,
                                             Authentication authentication) {
        requireAdmin(authentication);

        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("新密码不能为空");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);

        return ApiResponse.ok("密码已重置，该用户需重新登录");
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<String> setUserRole(@PathVariable Long id,
                                            @RequestBody UpdateRoleRequest request,
                                            Authentication authentication) {
        requireAdmin(authentication);

        if (request.getRole() == null || (request.getRole() != 0 && request.getRole() != 1)) {
            throw new RuntimeException("角色值无效，仅允许 0（普通用户）或 1（审核员）");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getRole() != null && user.getRole() >= 2) {
            throw new RuntimeException("不能修改管理员的角色");
        }

        String[] roleNames = {"普通用户", "审核员"};
        String oldRole = user.getRole() != null && user.getRole() < roleNames.length
                ? roleNames[user.getRole()] : "未知";
        String newRole = roleNames[request.getRole()];

        user.setRole(request.getRole());
        userRepository.save(user);

        return ApiResponse.ok("已将 " + user.getUsername() + " 从" + oldRole + "改为" + newRole);
    }

    @PutMapping("/users/{id}/ban")
    public ApiResponse<String> banUser(@PathVariable Long id,
                                       Authentication authentication) {
        requireAdmin(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getRole() != null && user.getRole() >= 2) {
            throw new RuntimeException("不能封禁管理员");
        }
        user.setStatus(1);
        user.setTokenVersion(user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userRepository.save(user);

        messageService.send(id, "BAN",
                "账号已被封禁",
                "您的账号因违反平台规定已被封禁，如有疑问请联系管理员。",
                null);
        return ApiResponse.ok("用户已封禁");
    }

    @PutMapping("/users/{id}/unban")
    public ApiResponse<String> unbanUser(@PathVariable Long id,
                                         Authentication authentication) {
        requireAdmin(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(0);
        userRepository.save(user);

        messageService.send(id, "UNBAN",
                "账号已解封",
                "您的账号已被解封，现在可以正常使用平台功能。",
                null);
        return ApiResponse.ok("用户已解封");
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<String> deleteUser(@PathVariable Long id,
                                          Authentication authentication) {
        requireAdmin(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getRole() != null && user.getRole() >= 2) {
            throw new RuntimeException("不能删除管理员");
        }
        userRepository.delete(user);
        return ApiResponse.ok("用户已删除");
    }

    // ==================== 权限校验 ====================

    private void requireReviewer(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user.getRole() == null || user.getRole() < 1) {
            throw new RuntimeException("无权操作，仅审核员或管理员可执行此操作");
        }
    }

    private void requireAdmin(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user.getRole() == null || user.getRole() < 2) {
            throw new RuntimeException("无权操作，仅管理员可执行此操作");
        }
    }

    private User getCurrentUser(Authentication authentication) {
        Long userId = (Long) authentication.getDetails();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    private UserDto toDto(User user) {
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
