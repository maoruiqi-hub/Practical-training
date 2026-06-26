package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.AssessmentMistakeStat;
import com.neu.CoursePlatform.dto.AssessmentScoreRecord;

import java.util.List;

/** 模块三向模块五暴露的只读数据服务。 */
public interface AssessmentDataService {
    List<AssessmentScoreRecord> getStudentScores(String studentId, String courseId);
    List<AssessmentMistakeStat> getClassMistakeStats(String courseId);
}
