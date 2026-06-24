package com.neu.CoursePlatform.dto;

import lombok.Data;

import java.util.List;

@Data
public class ExamSubmitRequest {
    private List<ExamAnswerItem> answers;
}
