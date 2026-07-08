package com.mytravel.travel.dto;

import lombok.Data;

/**
 * 搜索结果：匹配城市或景点。
 */
@Data
public class SearchResult {

    /** 类型：city / attraction */
    private String type;

    /** ID：城市 region.id 或景点 attraction.id */
    private Long id;

    /** 名称：城市名或景点名 */
    private String name;

    /** 城市 ID（景点所属城市，或城市自身 ID） */
    private Long regionId;

    /** 城市名称 */
    private String regionName;

    /** 所属省份 ID */
    private Long parentId;

    /** 所属省份名称 */
    private String parentName;

    /** 分类：attraction / food / photo_spot */
    private String category;
}
