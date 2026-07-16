package com.mytravel.message;

import com.mytravel.common.ApiResponse;
import com.mytravel.user.User;
import com.mytravel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    private Long getUserId(Authentication authentication) {
        return (Long) authentication.getDetails();
    }

    /** 获取当前用户的消息列表 */
    @GetMapping
    public ApiResponse<List<Message>> getMessages(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(messageService.getMessages(userId));
    }

    /** 获取未读消息数 */
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.ok(Map.of("count", messageService.getUnreadCount(userId)));
    }

    /** 全部标记为已读 */
    @PutMapping("/read-all")
    public ApiResponse<String> markAllAsRead(Authentication authentication) {
        Long userId = getUserId(authentication);
        messageService.markAllAsRead(userId);
        return ApiResponse.ok("ok");
    }

    /** 标记单条消息为已读 */
    @PutMapping("/{id}/read")
    public ApiResponse<String> markAsRead(@PathVariable Long id,
                                          Authentication authentication) {
        messageService.markAsRead(id);
        return ApiResponse.ok("ok");
    }
}
