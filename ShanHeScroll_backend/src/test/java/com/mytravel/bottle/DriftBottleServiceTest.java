package com.mytravel.bottle;

import com.mytravel.bottle.repository.DriftBottleRepository;
import com.mytravel.bottle.repository.BottleLikeRepository;
import com.mytravel.bottle.service.DriftBottleService;
import com.mytravel.common.util.RateLimiter;
import com.mytravel.user.User;
import com.mytravel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriftBottleServiceTest {

    @Mock
    private DriftBottleRepository bottleRepository;

    @Mock
    private BottleLikeRepository likeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RateLimiter rateLimiter;

    private DriftBottleService bottleService;

    @BeforeEach
    void setUp() {
        bottleService = new DriftBottleService(bottleRepository, likeRepository, userRepository, rateLimiter);
    }

    // ---- 发送 ----

    @Test
    void shouldSendBottleAsAnonymous() {
        when(rateLimiter.tryAcquireDaily(anyString(), anyLong(), anyInt())).thenReturn(true);
        when(bottleRepository.save(any(DriftBottle.class))).thenAnswer(inv -> {
            DriftBottle b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        DriftBottle result = bottleService.sendBottle(1L, "你好世界", null, 1);

        assertEquals(1, result.getIsAnonymous());
        assertEquals("你好世界", result.getTextContent());
        assertEquals(0, result.getStatus()); // 待审核
    }

    @Test
    void shouldSendBottleAsRealName() {
        when(rateLimiter.tryAcquireDaily(anyString(), anyLong(), anyInt())).thenReturn(true);
        when(bottleRepository.save(any(DriftBottle.class))).thenAnswer(inv -> {
            DriftBottle b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        DriftBottle result = bottleService.sendBottle(1L, "实名瓶子", null, 0);

        assertEquals(0, result.getIsAnonymous());
    }

    @Test
    void shouldRejectEmptyContent() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bottleService.sendBottle(1L, "", null, 0));
        assertTrue(ex.getMessage().contains("内容不能为空"));
    }

    // ---- 拾取 ----

    @Test
    void shouldPickBottleWithSenderInfo() {
        when(rateLimiter.tryAcquireDaily(anyString(), anyLong(), anyInt())).thenReturn(true);
        DriftBottle bottle = new DriftBottle();
        bottle.setId(1L);
        bottle.setSenderId(42L);
        bottle.setIsAnonymous(0); // 实名
        bottle.setTextContent("一条漂流");
        bottle.setStatus(1);

        when(bottleRepository.findRandomApproved()).thenReturn(Optional.of(bottle));
        when(likeRepository.countByBottleId(1L)).thenReturn(3L);

        User sender = new User();
        sender.setId(42L);
        sender.setUsername("sender1");
        sender.setNickname("发送者");
        sender.setAvatar("/uploads/avatar.jpg");
        when(userRepository.findById(42L)).thenReturn(Optional.of(sender));

        DriftBottle result = bottleService.pickBottle(99L);

        assertEquals(1, result.getStatus()); // 保持审核通过
        assertEquals(3L, result.getLikeCount());
        assertNotNull(result.getSender());
        assertEquals("发送者", result.getSender().getNickname());
    }

    @Test
    void shouldPickAnonymousBottleWithoutSenderInfo() {
        when(rateLimiter.tryAcquireDaily(anyString(), anyLong(), anyInt())).thenReturn(true);
        DriftBottle bottle = new DriftBottle();
        bottle.setId(2L);
        bottle.setSenderId(42L);
        bottle.setIsAnonymous(1);
        bottle.setStatus(1);

        when(bottleRepository.findRandomApproved()).thenReturn(Optional.of(bottle));
        when(likeRepository.countByBottleId(2L)).thenReturn(0L);

        DriftBottle result = bottleService.pickBottle(99L);

        assertNull(result.getSender()); // 匿名不显示发送者
    }

    @Test
    void shouldThrowWhenNoBottles() {
        when(rateLimiter.tryAcquireDaily(anyString(), anyLong(), anyInt())).thenReturn(true);
        when(bottleRepository.findRandomApproved()).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bottleService.pickBottle(99L));
        assertTrue(ex.getMessage().contains("没有可拾取的漂流瓶"));
    }

    // ---- 点赞 ----

    @Test
    void shouldLikeBottle() {
        DriftBottle bottle = new DriftBottle();
        bottle.setId(1L);

        when(bottleRepository.findById(1L)).thenReturn(Optional.of(bottle));
        when(likeRepository.existsByBottleIdAndUserId(1L, 99L)).thenReturn(false);

        assertDoesNotThrow(() -> bottleService.likeBottle(99L, 1L));
        verify(likeRepository).save(any(BottleLike.class));
    }

    @Test
    void shouldRejectDuplicateLike() {
        DriftBottle bottle = new DriftBottle();
        bottle.setId(1L);

        when(bottleRepository.findById(1L)).thenReturn(Optional.of(bottle));
        when(likeRepository.existsByBottleIdAndUserId(1L, 99L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bottleService.likeBottle(99L, 1L));
        assertTrue(ex.getMessage().contains("已经赞过"));
    }

    // ---- 删除 ----

    @Test
    void shouldDeleteOwnBottle() {
        DriftBottle bottle = new DriftBottle();
        bottle.setId(1L);
        bottle.setSenderId(1L);

        when(bottleRepository.findById(1L)).thenReturn(Optional.of(bottle));

        bottleService.deleteBottle(1L, 1L);

        verify(likeRepository).deleteByBottleId(1L);
        verify(bottleRepository).delete(bottle);
    }

    @Test
    void shouldRejectDeleteByNonOwner() {
        DriftBottle bottle = new DriftBottle();
        bottle.setId(1L);
        bottle.setSenderId(42L); // 所有者是 42

        when(bottleRepository.findById(1L)).thenReturn(Optional.of(bottle));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> bottleService.deleteBottle(99L, 1L));
        assertTrue(ex.getMessage().contains("无权删除"));
    }

    // ---- 列表 ----

    @Test
    void shouldGetMyBottlesWithLikeCount() {
        DriftBottle b1 = new DriftBottle();
        b1.setId(1L);
        b1.setTextContent("第一条");

        when(bottleRepository.findBySenderIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(b1));
        when(likeRepository.countByBottleId(1L)).thenReturn(5L);

        List<DriftBottle> result = bottleService.getMyBottles(1L);

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).getLikeCount());
    }
}
