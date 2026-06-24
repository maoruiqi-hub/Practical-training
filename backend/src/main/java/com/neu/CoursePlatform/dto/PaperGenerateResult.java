package com.neu.CoursePlatform.dto;

import com.neu.CoursePlatform.entity.Paper;
import com.neu.CoursePlatform.entity.Question;
import lombok.Data;

import java.util.List;

@Data
public class PaperGenerateResult {
    private Paper paper;
    private List<Question> questions;

    public PaperGenerateResult(Paper paper, List<Question> questions) {
        this.paper = paper;
        this.questions = questions;
    }
}
