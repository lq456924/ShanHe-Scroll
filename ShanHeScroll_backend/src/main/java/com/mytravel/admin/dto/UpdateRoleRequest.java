package com.mytravel.admin.dto;

import lombok.Data;

@Data
public class UpdateRoleRequest {
    /**
     * 0 = 普通用户，1 = 审核员
     */
    private Integer role;
}
