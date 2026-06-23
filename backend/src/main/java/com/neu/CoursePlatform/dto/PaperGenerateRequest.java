package com.neu.CoursePlatform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PaperGenerateRequest {
    /** random / knowledge / difficulty */
    private String strategy;
    private Integer count;
    private List<String> types;
    private List<String> knowledgePoints;
    private Integer difficultyMin;
    private Integer difficultyMax;
    /** 可选：按题型指定数量，例如 {"single": 5, "fill": 3} */
    private Map<String, Integer> typeCounts;
}
