package com.picturebook.framework.service;

import com.picturebook.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储服务（M32）
 *
 * 第一期：本地文件存储
 * 第二期扩展：OSS / MinIO
 *
 * 存储路径规则：{base}/{type}/{yyyyMMdd}/{uuid}.{ext}
 * 访问路径：{urlPrefix}/{type}/{yyyyMMdd}/{uuid}.{ext}
 *
 * 文件类型：
 *   cover       绘本封面
 *   illustration 绘本插画
 *   avatar      角色形象
 *   audio       讲读/对话音频
 *   ai_result   AI生成结果
 *   upload      用户上传
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${picturebook.file.storage-type:local}")
    private String storageType;

    @Value("${picturebook.file.local.base-path:./uploads}")
    private String localBasePath;

    @Value("${picturebook.file.local.url-prefix:/uploads}")
    private String urlPrefix;

    @Value("${picturebook.file.allowed-types:jpg,jpeg,png,gif,webp,mp3,wav,m4a,txt,md}")
    private String allowedTypes;

    /**
     * 上传文件
     *
     * @param file 上传的文件
     * @param type 文件类型
     * @return 访问URL
     */
    public String upload(MultipartFile file, String type) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        validateType(file);

        String safeType = (type == null || type.isBlank()) ? "upload" : type;
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String originalName = file.getOriginalFilename();
        String ext = getFileExtension(originalName);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relativePath = safeType + "/" + datePath + "/" + fileName;

        switch (storageType) {
            case "local":
                return uploadLocal(file, relativePath);
            case "oss":
                // TODO: 接入 OSS SDK
                return uploadLocal(file, relativePath);
            case "minio":
                // TODO: 接入 MinIO SDK
                return uploadLocal(file, relativePath);
            default:
                return uploadLocal(file, relativePath);
        }
    }

    /**
     * 本地存储
     */
    private String uploadLocal(MultipartFile file, String relativePath) {
        File baseDir = new File(localBasePath);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
        File dest = new File(baseDir, relativePath);
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            log.error("文件存储失败: {}", relativePath, e);
            throw new BusinessException("文件存储失败");
        }
        return urlPrefix + "/" + relativePath;
    }

    /**
     * 删除文件（本地）
     */
    public boolean delete(String url) {
        if (url == null || !url.startsWith(urlPrefix)) {
            return false;
        }
        String relative = url.substring(urlPrefix.length() + 1);
        File target = new File(localBasePath, relative);
        return target.exists() && target.delete();
    }

    /**
     * 校验文件类型
     */
    private void validateType(MultipartFile file) {
        String ext = getFileExtension(file.getOriginalFilename()).replace(".", "").toLowerCase();
        List<String> allowed = Arrays.asList(allowedTypes.toLowerCase().split(","));
        if (!allowed.contains(ext)) {
            throw new BusinessException("不支持的文件类型: " + ext);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public String getStorageType() {
        return storageType;
    }
}
