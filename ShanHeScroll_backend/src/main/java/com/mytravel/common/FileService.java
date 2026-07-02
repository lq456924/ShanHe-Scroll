package com.mytravel.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
    private static final int THUMB_MAX = 400; // 缩略图最大边长

    private final Path uploadDir;

    public FileService(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 保存上传文件，同时生成缩略图。
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
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path targetDir = uploadDir.resolve(datePath);
            Files.createDirectories(targetDir);

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            }
            String baseName = UUID.randomUUID().toString();

            // 保存原图
            Path targetPath = targetDir.resolve(baseName + ext);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // 生成缩略图（仅 jpg/png）
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")) {
                try {
                    BufferedImage original = ImageIO.read(targetPath.toFile());
                    if (original != null) {
                        BufferedImage thumb = resizeImage(original);
                        Path thumbPath = targetDir.resolve("thumb_" + baseName + ext);
                        ImageIO.write(thumb, ext.replace(".", ""), thumbPath.toFile());
                    }
                } catch (Exception ignored) {
                    // 缩略图生成失败不影响主流程
                }
            }

            return "/uploads/" + datePath + "/" + baseName + ext;

        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取缩略图 URL（根据原图 URL 推导）。
     * 使用约定：缩略图与原图同目录，文件名加 thumb_ 前缀。
     */
    public String getThumbUrl(String urlPath) {
        if (urlPath == null) return null;
        int lastSlash = urlPath.lastIndexOf('/');
        if (lastSlash < 0) return urlPath;
        String dir = urlPath.substring(0, lastSlash);
        String filename = urlPath.substring(lastSlash + 1);
        return dir + "/thumb_" + filename;
    }

    /**
     * 等比例缩放图片到 THUMB_MAX 范围内。
     */
    private BufferedImage resizeImage(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        if (w <= THUMB_MAX && h <= THUMB_MAX) return original;

        double ratio = (double) w / h;
        int newW, newH;
        if (w > h) {
            newW = THUMB_MAX;
            newH = (int) (THUMB_MAX / ratio);
        } else {
            newH = THUMB_MAX;
            newW = (int) (THUMB_MAX * ratio);
        }

        BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, newW, newH, null);
        g.dispose();
        return resized;
    }

    /**
     * 删除上传文件及其缩略图。
     */
    public void delete(String urlPath) {
        try {
            Path filePath = uploadDir.resolve(urlPath.replace("/uploads/", ""));
            Files.deleteIfExists(filePath);
            // 同时删除缩略图
            Path thumbPath = uploadDir.resolve(getThumbUrl(urlPath).replace("/uploads/", ""));
            Files.deleteIfExists(thumbPath);
        } catch (IOException e) {
            throw new RuntimeException("文件删除失败: " + e.getMessage(), e);
        }
    }
}
