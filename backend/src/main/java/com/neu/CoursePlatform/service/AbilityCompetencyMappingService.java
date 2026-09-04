package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.dto.AbilityCompetencyMapDTO;
import com.neu.CoursePlatform.dto.AbilityCompetencyRelationRequest;
import com.neu.CoursePlatform.entity.CompetencyPoint;
import com.neu.CoursePlatform.entity.CompetencyTaskObservation;

import java.util.Map;
import java.util.List;

public interface AbilityCompetencyMappingService {
    AbilityCompetencyMapDTO getByCourseCode(String courseCode);
    CompetencyPoint createCompetency(CompetencyPoint competency);
    CompetencyPoint getCompetencyById(String competencyId);
    boolean hasAbilityPoint(String abilityPointId, String courseCode);
    boolean hasTask(String taskNo, String courseCode);
    boolean updateCompetency(String competencyId, CompetencyPoint request);
    boolean deleteCompetency(String competencyId);
    void saveRelation(AbilityCompetencyRelationRequest request);
    void saveObservation(CompetencyTaskObservation observation);
    void saveObservations(List<CompetencyTaskObservation> observations);
    Map<String, Object> calibrateStrengths(String courseCode);
    void publishVersion(String courseCode, String version);
    void publishVersion(String courseCode, String version, String publisherId);
}
