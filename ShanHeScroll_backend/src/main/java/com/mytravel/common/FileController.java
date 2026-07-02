package com.mytravel.common;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 单文件上传。
     */
    @PostMapping("/upload")
    public ApiResponse<String> upload(@RequestParam("file") MultipartFile file) {
        String url = fileService.store(file);
        return ApiResponse.ok(url);
    }

    /**
     * 多文件上传（最多 9 张）。
     */
    @PostMapping("/upload/batch")
    public ApiResponse<List<String>> uploadBatch(@RequestParam("files") List<MultipartFile> files) {
        if (files.size() > 9) {
            throw new RuntimeException("单次最多上传 9 张图片");
        }
        List<String> urls = files.stream().map(fileService::store).toList();
        return ApiResponse.ok(urls);
    }
}
