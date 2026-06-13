package com.baking.util;

import com.baking.enums.ImageType;
import lombok.Data;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ImageUtil {

    @Value("${baking.upload.path}")
    private String uploadPath;

    @Value("${baking.upload.url-prefix}")
    private String urlPrefix;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Data
    public static class UploadResult {
        private String url;
        private String thumbnailUrl;
        private String type;
        private int width;
        private int height;

        public Map<String, String> toMap() {
            Map<String, String> map = new HashMap<>();
            map.put("url", url);
            map.put("type", type);
            if (thumbnailUrl != null) {
                map.put("thumbnailUrl", thumbnailUrl);
            }
            map.put("width", String.valueOf(width));
            map.put("height", String.valueOf(height));
            return map;
        }
    }

    public UploadResult uploadImage(MultipartFile file, ImageType imageType) throws IOException {
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

        InputStream inputStream = file.getInputStream();
        Thumbnails.Builder<? extends InputStream> builder = Thumbnails.of(inputStream)
                .size(imageType.getMaxWidth(), imageType.getMaxHeight())
                .outputQuality(imageType.getQuality());

        if (".png".equalsIgnoreCase(extension)) {
            builder.outputFormat("png");
        }

        builder.toFile(targetPath.toFile());

        String externalPrefix = contextPath + urlPrefix;

        UploadResult result = new UploadResult();
        result.setUrl(externalPrefix + "/" + dateDir + "/" + fileName);
        result.setType(imageType.getCode());

        int[] dimensions = getImageDimensions(targetPath.toFile());
        result.setWidth(dimensions[0]);
        result.setHeight(dimensions[1]);

        if (imageType.isGenerateThumbnail()) {
            Path thumbDir = Paths.get(uploadPath, dateDir, "thumb");
            if (!Files.exists(thumbDir)) {
                Files.createDirectories(thumbDir);
            }
            String thumbFileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path thumbPath = thumbDir.resolve(thumbFileName);

            Thumbnails.of(targetPath.toFile())
                    .size(imageType.getThumbWidth(), imageType.getThumbHeight())
                    .outputQuality(imageType.getQuality())
                    .toFile(thumbPath.toFile());

            result.setThumbnailUrl(externalPrefix + "/" + dateDir + "/thumb/" + thumbFileName);
        }

        return result;
    }

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, true);
    }

    public String uploadImage(MultipartFile file, boolean compress) throws IOException {
        if (!compress) {
            UploadResult result = uploadImage(file, ImageType.GENERAL);
            return result.getUrl();
        }
        UploadResult result = uploadImage(file, ImageType.GENERAL);
        return result.getUrl();
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

        return contextPath + urlPrefix + "/" + dateDir + "/thumb/" + fileName;
    }

    private int[] getImageDimensions(File file) {
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(file);
            if (image != null) {
                return new int[]{image.getWidth(), image.getHeight()};
            }
        } catch (IOException e) {
            // ignore
        }
        return new int[]{0, 0};
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
