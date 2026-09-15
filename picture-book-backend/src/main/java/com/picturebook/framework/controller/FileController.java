package com.picturebook.framework.controller;

import com.picturebook.common.core.Result;
import com.picturebook.framework.service.FileStorageService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传接口（M32）
 * POST /api/file/upload
 * 入参 {file, type}，返回 {url}
 */
@RestController
@RequestMapping("/api/file")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 文件上传
     *
     * @param file 文件
     * @param type 文件类型 cover/illustration/avatar/audio/ai_result/upload
     * @return 访问URL
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "type", defaultValue = "upload") String type) {
        String url = fileStorageService.upload(file, type);
        return Result.ok(url);
    }
}
