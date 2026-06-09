package com.baking.util;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class ImageUtil {

    @Value("${baking.upload.path}")
    private String uploadPath;

    @Value("${baking.upload.url-prefix}")
    private String urlPrefix;

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, true);
    }

    public String uploadImage(MultipartFile file, boolean compress) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadPath, dateDir);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetPath = targetDir.resolve(fileName);

        if (compress && isImageFile(extension)) {
            Thumbnails.of(file.getInputStream())
                    .size(1920, 1920)
                    .outputQuality(0.8)
                    .toFile(targetPath.toFile());
        } else {
            file.transferTo(targetPath.toFile());
        }

        return urlPrefix + "/" + dateDir + "/" + fileName;
    }

    public String uploadThumbnail(MultipartFile file, int width, int height) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ?
                originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadPath, dateDir, "thumb");
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path targetPath = targetDir.resolve(fileName);

        Thumbnails.of(file.getInputStream())
                .size(width, height)
                .outputQuality(0.8)
                .toFile(targetPath.toFile());

        return urlPrefix + "/" + dateDir + "/thumb/" + fileName;
    }

    private boolean isImageFile(String extension) {
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp"};
        for (String ext : imageExtensions) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }
}
