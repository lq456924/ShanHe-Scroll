package com.mytravel.album.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdatePrivacyRequest {
    /** 可见性: 0=所有人  1=仅自己  2=白名单  3=黑名单 */
    private Integer visibility;
    /** 白名单/黑名单用户 ID 列表（仅 visibility=2 或 3 时需要） */
    private List<Long> userIds;
}
