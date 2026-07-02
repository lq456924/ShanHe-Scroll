package com.mytravel.album;

import lombok.Data;

@Data
public class CreateAlbumRequest {
    private Long regionId;
    private String title;
    private String description;
    private String coverImage;
}
