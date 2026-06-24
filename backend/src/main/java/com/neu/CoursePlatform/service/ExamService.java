package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.dto.ExamGenerateRequest;
import com.neu.CoursePlatform.dto.ExamGenerateResult;
import com.neu.CoursePlatform.dto.ExamDetailDTO;
import com.neu.CoursePlatform.entity.Exam;

import java.util.List;

public interface ExamService extends IService<Exam> {

    ExamGenerateResult generateAndSave(String courseCode, ExamGenerateRequest request);

    void bindToTask(String examId, String taskNo);

    List<Exam> listByCourseCode(String courseCode);

    ExamDetailDTO getDetail(String examId);
}
