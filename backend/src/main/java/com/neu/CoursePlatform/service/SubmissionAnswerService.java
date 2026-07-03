package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.SubmissionAnswer;

import java.util.List;

public interface SubmissionAnswerService extends IService<SubmissionAnswer> {

    List<SubmissionAnswer> listBySubmissionId(String submissionId);

    List<SubmissionAnswer> listByStudentNo(String studentNo, String taskNo, String knowledgePointId, String type);

    List<SubmissionAnswer> listByStudentNoAndCourse(String studentNo, String courseCode, String taskNo, String knowledgePointId, String type);

    List<SubmissionAnswer> listWrongByStudentNo(String studentNo);

    List<SubmissionAnswer> listByTaskNo(String taskNo);
}
