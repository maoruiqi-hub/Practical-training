package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.MistakeStatsDTO;
import com.neu.CoursePlatform.dto.ScoreRecordDTO;
import com.neu.CoursePlatform.entity.SubmissionAnswer;

import java.util.List;

/** Read-only Module 3 contract for Module 4, Module 5 and the tower UI. */
public interface AssessmentQueryService {
    List<ScoreRecordDTO> getStudentScores(String studentNo, String courseCode);
    List<SubmissionAnswer> getStudentMistakes(String studentNo, String courseCode, String knowledgePointId);
    List<MistakeStatsDTO> getCourseMistakeStats(String courseCode);
    List<ScoreRecordDTO> getExamScores(String examId);
}
