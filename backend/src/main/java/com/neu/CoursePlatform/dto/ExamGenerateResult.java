package com.neu.CoursePlatform.dto;

import com.neu.CoursePlatform.entity.Exam;
import com.neu.CoursePlatform.entity.Question;
import lombok.Data;

import java.util.List;

@Data
public class ExamGenerateResult {
    private Exam exam;
    private List<Question> questions;

    public ExamGenerateResult(Exam exam, List<Question> questions) {
        this.exam = exam;
        this.questions = questions;
    }
}
