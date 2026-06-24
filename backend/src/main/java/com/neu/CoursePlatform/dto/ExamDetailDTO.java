package com.neu.CoursePlatform.dto;

import com.neu.CoursePlatform.entity.Exam;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ExamDetailDTO {
    private Exam exam;
    private List<ExamQuestionViewDTO> questions;
}
