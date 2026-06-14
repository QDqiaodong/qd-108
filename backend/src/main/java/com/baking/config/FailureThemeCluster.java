package com.baking.config;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

public class FailureThemeCluster {

    @Getter
    public enum FailureTheme {
        SHAPE_COLLAPSE("形状塌陷", "塌腰、塌陷、回缩等形状问题", Arrays.asList(
                "塌腰", "塌陷", "回缩", "凹陷", "塌了", "扁了", "变形", "不挺立", "中间塌", "中间凹陷"
        )),
        FERMENTATION_ISSUE("发酵问题", "发酵不足、过度等发酵相关问题", Arrays.asList(
                "发酵不足", "发酵慢", "发酵偏慢", "发酵太慢", "发不起来", "发不起", "没发起来",
                "发酵过度", "发过了", "发酵快", "发酵偏快", "发酵太快", "发酵过头"
        )),
        BROWNING_ISSUE("上色问题", "不上色、上色不均等颜色问题", Arrays.asList(
                "不上色", "上色浅", "上色偏浅", "颜色浅", "发白", "没上色",
                "上色深", "上色偏深", "颜色深", "烤焦", "烤糊", "糊了", "焦了", "上色不均", "颜色不均"
        )),
        TEXTURE_ISSUE("组织问题", "组织粗糙、气孔大等内部组织问题", Arrays.asList(
                "组织粗糙", "气孔大", "孔洞大", "不细腻", "粗糙", "内部粗糙",
                "太硬", "偏硬", "硬邦邦", "过硬",
                "太软", "偏软", "过软", "发黏",
                "太干", "偏干", "干燥", "发干",
                "太湿", "偏湿", "粘手", "潮湿"
        )),
        CRACK_ISSUE("开裂问题", "表面开裂、裂口等问题", Arrays.asList(
                "开裂", "裂口", "裂了", "表面裂", "顶部裂", "侧面裂", "裂纹"
        )),
        TASTE_ISSUE("味道问题", "太甜、太咸、有腥味等味道问题", Arrays.asList(
                "太甜", "过甜", "偏甜", "糖多",
                "太咸", "过咸", "偏咸", "盐多",
                "腥", "腥味", "蛋腥",
                "酸", "酸味", "发酸",
                "没味道", "淡", "偏淡", "无味"
        )),
        STICKING_ISSUE("粘模问题", "粘模、脱模困难等问题", Arrays.asList(
                "粘模", "粘盘", "脱不了模", "脱模失败", "粘底", "撕坏", "破皮", "破了"
        )),
        DONENESS_ISSUE("熟度问题", "不熟、烤不透等问题", Arrays.asList(
                "不熟", "没熟", "烤不熟", "烤不透", "中间不熟", "夹生",
                "烤过了", "烤太干", "烤老了"
        )),
        MOISTURE_ISSUE("水分问题", "出油、出水等水分异常问题", Arrays.asList(
                "出油", "漏油", "出水", "渗水", "析水", "太油", "油腻"
        ));

        private final String themeName;
        private final String description;
        private final List<String> keywords;

        FailureTheme(String themeName, String description, List<String> keywords) {
            this.themeName = themeName;
            this.description = description;
            this.keywords = keywords;
        }
    }

    private FailureThemeCluster() {
    }
}
