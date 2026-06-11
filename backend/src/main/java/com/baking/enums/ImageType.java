package com.baking.enums;

import lombok.Getter;

@Getter
public enum ImageType {

    COVER("cover", "封面图", 1600, 1600, 0.85f, 400, 400, true),
    STEP("step", "步骤图", 1200, 1200, 0.75f, 0, 0, false),
    RESULT("result", "成品展示图", 1920, 1920, 0.90f, 600, 600, true),
    AVATAR("avatar", "用户头像", 512, 512, 0.80f, 128, 128, true),
    GENERAL("general", "通用图", 1920, 1920, 0.80f, 0, 0, false);

    private final String code;
    private final String description;
    private final int maxWidth;
    private final int maxHeight;
    private final float quality;
    private final int thumbWidth;
    private final int thumbHeight;
    private final boolean generateThumbnail;

    ImageType(String code, String description, int maxWidth, int maxHeight,
              float quality, int thumbWidth, int thumbHeight, boolean generateThumbnail) {
        this.code = code;
        this.description = description;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.quality = quality;
        this.thumbWidth = thumbWidth;
        this.thumbHeight = thumbHeight;
        this.generateThumbnail = generateThumbnail;
    }

    public static ImageType fromCode(String code) {
        if (code == null) {
            return GENERAL;
        }
        for (ImageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return GENERAL;
    }
}
