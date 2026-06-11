package com.baking.dto;

import lombok.Data;

@Data
public class CategoryBakeStats {
    private Long categoryId;
    private String categoryName;
    private Integer tempMin;
    private Integer tempMax;
    private Integer tempP25;
    private Integer tempP75;
    private Double tempAvg;
    private Integer timeMin;
    private Integer timeMax;
    private Integer timeP25;
    private Integer timeP75;
    private Double timeAvg;
    private Integer sampleCount;

    public boolean isTempDeviated(Integer temp) {
        if (temp == null || tempP25 == null || tempP75 == null) return false;
        return temp < tempP25 - 10 || temp > tempP75 + 10;
    }

    public boolean isTimeDeviated(Integer time) {
        if (time == null || timeP25 == null || timeP75 == null) return false;
        return time < timeP25 - 5 || time > timeP75 + 5;
    }

    public String getTempDeviationNote(Integer temp) {
        if (temp == null || !isTempDeviated(temp)) return null;
        if (temp < tempP25 - 10) {
            return "温度偏低，" + categoryName + "类常见温度区间为 " + tempP25 + "~" + tempP75 + "℃";
        } else {
            return "温度偏高，" + categoryName + "类常见温度区间为 " + tempP25 + "~" + tempP75 + "℃";
        }
    }

    public String getTimeDeviationNote(Integer time) {
        if (time == null || !isTimeDeviated(time)) return null;
        if (time < timeP25 - 5) {
            return "时长偏短，" + categoryName + "类常见时长区间为 " + timeP25 + "~" + timeP75 + "分钟";
        } else {
            return "时长偏长，" + categoryName + "类常见时长区间为 " + timeP25 + "~" + timeP75 + "分钟";
        }
    }
}
