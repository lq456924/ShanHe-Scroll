package com.mytravel.bottle.dto;

import com.mytravel.bottle.DriftBottle;
import lombok.Data;

@Data
public class BottleDetailResponse {
    private DriftBottle bottle;
    /** 当前用户是否已点赞 */
    private boolean liked;
}
