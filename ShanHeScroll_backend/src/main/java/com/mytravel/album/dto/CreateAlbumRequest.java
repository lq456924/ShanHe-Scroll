package com.mytravel.album.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateAlbumRequest {
    private Long regionId;
    private String title;
    private String description;
    private String coverImage;
    /** 是否为多人共同相册 */
    private Boolean isShared = false;
    /** 邀请的用户 ID 列表（仅 isShared=true 时有效） */
    private List<Long> inviteeIds;
}
