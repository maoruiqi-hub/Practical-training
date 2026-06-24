package com.neu.CoursePlatform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ExamGenerateRequest {
    /** random / knowledge / difficulty */
    private String strategy;
    private Integer count;
    private List<String> types;
    /** 按知识点实体 ID 过滤 */
    private List<String> knowledgePointIds;
    private Integer difficultyMin;
    private Integer difficultyMax;
    /** 可选：目标总分，使用题目默认分值汇总校验 */
    private Integer targetScore;
    /** 可选：按题型指定数量，例如 {"single": 5, "fill": 3} */
    private Map<String, Integer> typeCounts;
    /** 可选：按知识点实体 ID 指定数量，例如 {"7": 3, "4": 2} */
    private Map<String, Integer> knowledgePointIdCounts;
    /** 可选：按难度比例组卷，例如 {"1": 40, "3": 40, "5": 20} */
    private Map<Integer, Integer> difficultyRatios;
}
