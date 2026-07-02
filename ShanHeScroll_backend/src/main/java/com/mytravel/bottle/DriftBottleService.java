package com.mytravel.bottle;

import com.mytravel.common.util.RateLimiter;
import com.mytravel.user.User;
import com.mytravel.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DriftBottleService {

    private final DriftBottleRepository bottleRepository;
    private final BottleLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final RateLimiter rateLimiter;

    // ==================== 发送 ====================

    /**
     * 发送漂流瓶。新瓶子默认审核通过（后续管理员审核功能上线后再改）。
     */
    public DriftBottle sendBottle(Long senderId, String textContent,
                                  String imageUrl, Integer isAnonymous) {
        if (textContent == null || textContent.isBlank()) {
            throw new RuntimeException("漂流瓶内容不能为空");
        }
        if (isAnonymous == null) {
            isAnonymous = 0;
        }

        // 每人每天最多发送 3 个
        if (!rateLimiter.tryAcquireDaily("bottle-send:" + senderId, 24 * 60 * 60_000L, 3)) {
            throw new RuntimeException("今天已发送 3 个漂流瓶，明天再来吧");
        }

        DriftBottle bottle = new DriftBottle();
        bottle.setSenderId(senderId);
        bottle.setTextContent(textContent);
        bottle.setImageUrl(imageUrl);
        bottle.setIsAnonymous(isAnonymous);
        bottle.setStatus(0); // 待审核，需审核员通过后才能被拾取

        return bottleRepository.save(bottle);
    }

    // ==================== 拾取 ====================

    /**
     * 随机拾取一个审核通过的漂流瓶，拾取后状态改为"已拾取"。
     * 根据匿名设置返回发送者信息。
     */
    @Transactional
    public DriftBottle pickBottle(Long viewerId) {
        // 每人每天最多拾取 10 个
        if (!rateLimiter.tryAcquireDaily("bottle-pick:" + viewerId, 24 * 60 * 60_000L, 10)) {
            throw new RuntimeException("今天已拾取 10 个漂流瓶，明天再来吧");
        }

        DriftBottle bottle = bottleRepository.findRandomApproved()
                .orElseThrow(() -> new RuntimeException("暂时没有可拾取的漂流瓶"));

        // 不再把状态改为3，允许被重复拾取

        // 填充点赞数
        bottle.setLikeCount(likeRepository.countByBottleId(bottle.getId()));

        // 非匿名 → 显示发送者信息
        if (bottle.getIsAnonymous() == 0) {
            User sender = userRepository.findById(bottle.getSenderId()).orElse(null);
            if (sender != null) {
                DriftBottle.SenderInfo info = new DriftBottle.SenderInfo();
                info.setId(sender.getId());
                info.setUsername(sender.getUsername());
                info.setNickname(sender.getNickname());
                info.setAvatar(sender.getAvatar());
                bottle.setSender(info);
            }
        }

        return bottle;
    }

    // ==================== 点赞 ====================

    public void likeBottle(Long userId, Long bottleId) {
        DriftBottle bottle = bottleRepository.findById(bottleId)
                .orElseThrow(() -> new RuntimeException("漂流瓶不存在"));

        if (likeRepository.existsByBottleIdAndUserId(bottleId, userId)) {
            throw new RuntimeException("你已经赞过这个漂流瓶了");
        }

        BottleLike like = new BottleLike();
        like.setBottleId(bottleId);
        like.setUserId(userId);
        likeRepository.save(like);
    }

    /**
     * 取消点赞。
     */
    @Transactional
    public void unlikeBottle(Long userId, Long bottleId) {
        likeRepository.findByBottleIdAndUserId(bottleId, userId)
                .ifPresent(likeRepository::delete);
    }

    // ==================== 删除 ====================

    /**
     * 删除自己发出的漂流瓶（仅所有者可操作）。
     */
    @Transactional
    public void deleteBottle(Long userId, Long bottleId) {
        DriftBottle bottle = bottleRepository.findById(bottleId)
                .orElseThrow(() -> new RuntimeException("漂流瓶不存在"));
        if (!bottle.getSenderId().equals(userId)) {
            throw new RuntimeException("无权删除此漂流瓶");
        }
        likeRepository.deleteByBottleId(bottleId);
        bottleRepository.delete(bottle);
    }

    // ==================== 查询 ====================

    /** 我的漂流瓶列表 */
    public List<DriftBottle> getMyBottles(Long userId) {
        List<DriftBottle> bottles = bottleRepository.findBySenderIdOrderByCreatedAtDesc(userId);
        bottles.forEach(b -> b.setLikeCount(likeRepository.countByBottleId(b.getId())));
        return bottles;
    }

    /** 我点赞过的漂流瓶 */
    public List<DriftBottle> getMyLikedBottles(Long userId) {
        List<DriftBottle> bottles = bottleRepository.findLikedByUserId(userId);
        bottles.forEach(b -> b.setLikeCount(likeRepository.countByBottleId(b.getId())));
        return bottles;
    }

    /** 漂流瓶详情 */
    public DriftBottle getBottleDetail(Long bottleId) {
        DriftBottle bottle = bottleRepository.findById(bottleId)
                .orElseThrow(() -> new RuntimeException("漂流瓶不存在"));
        bottle.setLikeCount(likeRepository.countByBottleId(bottle.getId()));
        return bottle;
    }

    /** 是否已点赞 */
    public boolean isLikedByUser(Long bottleId, Long userId) {
        return likeRepository.existsByBottleIdAndUserId(bottleId, userId);
    }

    // ==================== 举报 ====================

    @Transactional
    public void reportBottle(Long reporterId, Long bottleId) {
        DriftBottle bottle = bottleRepository.findById(bottleId)
                .orElseThrow(() -> new RuntimeException("漂流瓶不存在"));
        // 改为待审核状态，重新进入审核队列
        bottle.setStatus(0);
        bottleRepository.save(bottle);
    }

    // ==================== 审核 ====================

    /** 获取待审核的漂流瓶列表 */
    public List<DriftBottle> getPendingBottles() {
        List<DriftBottle> bottles = bottleRepository.findByStatusOrderByCreatedAtDesc(0);
        bottles.forEach(b -> b.setLikeCount(likeRepository.countByBottleId(b.getId())));
        return bottles;
    }

    /** 审核通过 */
    @Transactional
    public void approveBottle(Long bottleId) {
        DriftBottle bottle = bottleRepository.findById(bottleId)
                .orElseThrow(() -> new RuntimeException("漂流瓶不存在"));
        if (bottle.getStatus() != 0) {
            throw new RuntimeException("该漂流瓶不是待审核状态");
        }
        bottle.setStatus(1);
        bottleRepository.save(bottle);
    }

    /** 审核拒绝 */
    @Transactional
    public void rejectBottle(Long bottleId) {
        DriftBottle bottle = bottleRepository.findById(bottleId)
                .orElseThrow(() -> new RuntimeException("漂流瓶不存在"));
        if (bottle.getStatus() != 0) {
            throw new RuntimeException("该漂流瓶不是待审核状态");
        }
        bottle.setStatus(2);
        bottleRepository.save(bottle);
    }
}
