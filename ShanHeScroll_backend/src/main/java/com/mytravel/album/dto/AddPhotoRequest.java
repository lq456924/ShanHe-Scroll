package com.mytravel.album.dto;

import lombok.Data;

@Data
public class AddPhotoRequest {
    private String url;
    private String description;
}
