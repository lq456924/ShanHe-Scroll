package com.mytravel.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    private FileService fileService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileService = new FileService(tempDir.toString());
    }

    @Test
    void shouldStoreImageAndReturnUrl() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "test image content".getBytes()
        );

        String url = fileService.store(file);

        assertTrue(url.startsWith("/uploads/"));
        assertTrue(url.endsWith(".jpg"));
        // 验证文件确实存在
        Path savedPath = tempDir.resolve(url.replace("/uploads/", ""));
        assertTrue(Files.exists(savedPath));
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.store(file));
        assertTrue(ex.getMessage().contains("文件不能为空"));
    }

    @Test
    void shouldRejectUnsupportedType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "fake pdf".getBytes()
        );

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.store(file));
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void shouldGenerateUniqueFilenames() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "content".getBytes()
        );

        String url1 = fileService.store(file);
        String url2 = fileService.store(file);

        assertNotEquals(url1, url2);
    }

    @Test
    void shouldDeleteFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "content".getBytes()
        );
        String url = fileService.store(file);

        fileService.delete(url);

        Path savedPath = tempDir.resolve(url.replace("/uploads/", ""));
        assertFalse(Files.exists(savedPath));
    }

    @Test
    void shouldHandlePngAndGif() {
        MockMultipartFile png = new MockMultipartFile(
                "file", "icon.png", "image/png", "png content".getBytes()
        );
        MockMultipartFile gif = new MockMultipartFile(
                "file", "anim.gif", "image/gif", "gif content".getBytes()
        );

        assertDoesNotThrow(() -> fileService.store(png));
        assertDoesNotThrow(() -> fileService.store(gif));
    }
}
