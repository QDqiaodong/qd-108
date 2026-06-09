package com.baking.controller;

import com.baking.common.Result;
import com.baking.util.ImageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class UploadController {

    private final ImageUtil imageUtil;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = imageUtil.uploadImage(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/thumbnail")
    public Result<Map<String, String>> uploadThumbnail(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "400") int width,
            @RequestParam(defaultValue = "400") int height) {
        try {
            String url = imageUtil.uploadThumbnail(file, width, height);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("缩略图上传失败: " + e.getMessage());
        }
    }
}
