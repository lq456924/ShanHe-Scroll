package com.mytravel.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10 MB

    private final Path uploadDir;

    public FileService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 保存上传文件，返回访问 URL 路径。
     *
     * @param file 上传文件
     * @return URL 相对路径，如 /uploads/2026/06/13/a1b2c3d4.jpg
     */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new RuntimeException("文件大小不能超过 10MB");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new RuntimeException("不支持的文件类型，仅允许 jpg / png / gif / webp");
        }

        try {
            // 按日期分目录：uploads/yyyy/MM/dd/
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = uploadDir.resolve(datePath);
            Files.createDirectories(targetDir);

            // 文件名：UUID + 原始扩展名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + ext;

            // 写入文件
            Path targetPath = targetDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 返回 URL 相对路径
            return "/uploads/" + datePath + "/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除上传文件。
     */
    public void delete(String urlPath) {
        try {
            Path filePath = uploadDir.resolve(urlPath.replace("/uploads/", ""));
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }
}
