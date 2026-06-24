package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.AbilityMapDTO;

public interface AbilityMapService {
    AbilityMapDTO getByCourseCode(String courseCode);
    boolean bindKnowledgePoint(String abilityPointId, String knowledgePointId);
    boolean unbindKnowledgePoint(String abilityPointId, String knowledgePointId);
    boolean deleteAbilityPoint(String abilityPointId);
}
