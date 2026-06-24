package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.entity.Exam;

public interface ExamService extends IService<Exam> {

    ExamGenerateResult generateAndSave(String courseCode, ExamGenerateRequest request);

    void bindToTask(String examId, String taskNo);
}
