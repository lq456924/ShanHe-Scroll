package com.mytravel.bottle.dto;

import lombok.Data;

@Data
public class SendBottleRequest {
    private String textContent;
    private String imageUrl;
    /** 是否匿名: 0=实名  1=匿名，默认实名 */
    private Integer isAnonymous;
}
