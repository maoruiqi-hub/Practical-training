package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAnswer;

import java.util.List;

/** Emits only facts from Module 3; Module 4 remains the owner of game rules. */
public interface TowerAssessmentEventService {
    void publishAssessmentEvents(LearningTask task, String studentNo, String submissionId,
                                 List<SubmissionAnswer> answers);
}
