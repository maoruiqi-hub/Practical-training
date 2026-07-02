package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.agentic.AgenticResponse;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseQaRequest;
import com.neu.CoursePlatform.dto.LectureRequest;

public interface CourseAiService {
    Result<AgenticResponse> explainKnowledgePoint(String knowledgePointId, LectureRequest request);

    Result<AgenticResponse> answerKnowledgePointQuestion(String knowledgePointId, CourseQaRequest request);

    Result<AgenticResponse> generateAbilityMap(String courseCode);
}
