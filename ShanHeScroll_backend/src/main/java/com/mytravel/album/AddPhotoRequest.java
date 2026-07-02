package com.mytravel.album;

import lombok.Data;

@Data
public class AddPhotoRequest {
    private String url;
    private String description;
}
