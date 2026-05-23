package cn.rbac.server.framework.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /** 本地存储根目录 */
    private String localPath = "./data/uploads";

    /** 单文件最大 MB */
    private int maxSizeMb = 50;

    /** 允许扩展名，逗号分隔；空表示不限制 */
    private String allowedExtensions =
            "jpg,jpeg,png,gif,webp,bmp,svg,pdf,doc,docx,xls,xlsx,ppt,pptx,txt,md,json,xml,zip,rar,mp4,mp3,wav,avi,mov";
}
