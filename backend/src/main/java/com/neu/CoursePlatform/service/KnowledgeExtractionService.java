package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.entity.KnowledgeExtractionCandidate;

import java.util.List;

/** Coordinates AI extraction and the teacher review workflow. */
public interface KnowledgeExtractionService {

    List<KnowledgeExtractionCandidate> extract(String courseCode, String resourceId);

    List<KnowledgeExtractionCandidate> listPending(String courseCode);

    String accept(String courseCode, String candidateId, KnowledgeExtractionCandidate editedCandidate);

    String reject(String courseCode, String candidateId);
}
