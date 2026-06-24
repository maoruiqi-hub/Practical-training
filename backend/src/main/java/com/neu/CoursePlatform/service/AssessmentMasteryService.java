package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.SubmissionAnswer;

import java.util.List;

/** Module 3's in-process bridge for sending objective assessment evidence to Module 1. */
public interface AssessmentMasteryService {
    void refreshFromObjectiveAnswers(String studentNo, String courseCode,
                                     List<SubmissionAnswer> latestAnswers, String sourceId);
}
