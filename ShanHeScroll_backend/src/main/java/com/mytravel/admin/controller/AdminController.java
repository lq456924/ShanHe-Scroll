package com.mytravel.admin.controller;

import com.mytravel.bottle.DriftBottle;
import com.mytravel.bottle.service.DriftBottleService;
import com.mytravel.common.ApiResponse;
import com.mytravel.travel.Attraction;
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
    private final TravelService travelService;
    private final UserRepository userRepository;
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
        return ApiResponse.ok("审核通过");
    }

    @PutMapping("/attractions/{id}/reject")
    public ApiResponse<String> rejectAttraction(@PathVariable Long id,
                                                 Authentication authentication) {
        requireReviewer(authentication);
        travelService.rejectAttraction(id);
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
        return ApiResponse.ok("审核通过");
    }

    @PutMapping("/bottles/{id}/reject")
    public ApiResponse<String> reject(@PathVariable Long id,
                                      Authentication authentication) {
        requireReviewer(authentication);
        bottleService.rejectBottle(id);
        return ApiResponse.ok("已拒绝");
    }

    // ==================== 用户管理（仅管理员） ====================

    /** 用户列表 */
    @GetMapping("/users")
    public ApiResponse<List<UserDto>> userList(Authentication authentication) {
        requireAdmin(authentication);
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserDto> dtos = users.stream().map(this::toDto).toList();
        return ApiResponse.ok(dtos);
    }

    /** 创建用户（可指定角色） */
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

    /** 重置用户密码 */
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

    /** 修改用户角色（仅管理员，且目标用户不能是管理员） */
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

    /** 封禁用户 */
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
        return ApiResponse.ok("用户已封禁");
    }

    /** 解封用户 */
    @PutMapping("/users/{id}/unban")
    public ApiResponse<String> unbanUser(@PathVariable Long id,
                                         Authentication authentication) {
        requireAdmin(authentication);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(0);
        userRepository.save(user);
        return ApiResponse.ok("用户已解封");
    }

    /** 删除用户（永久，不可恢复） */
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

    /** 审核员及以上：role >= 1 */
    private void requireReviewer(Authentication authentication) {
        User user = getCurrentUser(authentication);
        if (user.getRole() == null || user.getRole() < 1) {
            throw new RuntimeException("无权操作，仅审核员或管理员可执行此操作");
        }
    }

    /** 仅管理员：role == 2 */
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
