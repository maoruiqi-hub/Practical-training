package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.dto.AbilityCompetencyMapDTO;
import com.neu.CoursePlatform.entity.AbilityPointCompetencyRelation;
import com.neu.CoursePlatform.entity.CompetencyPoint;
import com.neu.CoursePlatform.service.AbilityCompetencyMappingService;
import com.neu.CoursePlatform.service.AbilitySnapshotService;
import com.neu.CoursePlatform.service.StudentAbilityProjectionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentAbilityProjectionServiceImpl implements StudentAbilityProjectionService {
    private final AbilitySnapshotService snapshotService;
    private final AbilityCompetencyMappingService mappingService;

    public StudentAbilityProjectionServiceImpl(AbilitySnapshotService snapshotService,
                                               AbilityCompetencyMappingService mappingService) {
        this.snapshotService = snapshotService;
        this.mappingService = mappingService;
    }

    @Override
    public List<Map<String, Object>> coursePoints(String studentNo, String courseCode) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AbilitySnapshotService.AbilityScore score : snapshotService.currentScores(studentNo, courseCode)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("abilityPointId", score.abilityPointId());
            item.put("name", score.name());
            item.put("description", score.description());
            item.put("score", score.score());
            item.put("hasEvidence", score.score() != null);
            item.put("evidenceCount", score.evidenceKnowledgeCount());
            item.put("totalKnowledgeCount", score.totalKnowledgeCount());
            item.put("coverageRate", score.totalKnowledgeCount() == 0 ? 0D
                    : round(score.evidenceKnowledgeCount() / (double) score.totalKnowledgeCount()));
            item.put("formulaVersion", FORMULA_VERSION);
            item.put("knowledgePointIds", score.knowledgePointIds());
            item.put("weights", score.weights());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> trueCompetencies(String studentNo, String courseCode) {
        AbilityCompetencyMapDTO mapping = mappingService.getByCourseCode(courseCode);
        Map<String, Map<String, Object>> coursePointIndex = new HashMap<>();
        coursePoints(studentNo, courseCode).forEach(item ->
                coursePointIndex.put(String.valueOf(item.get("abilityPointId")), item));

        List<Map<String, Object>> result = new ArrayList<>();
        for (CompetencyPoint competency : mapping.getCompetencies()) {
            if (!"active".equalsIgnoreCase(competency.getStatus())) continue;
            List<AbilityPointCompetencyRelation> relations = mapping.getRelations().stream()
                    .filter(item -> competency.getCompetencyId().equals(item.getCompetencyId())
                            && "related".equalsIgnoreCase(item.getRelationStatus())
                            && item.getStrength() != null && item.getStrength().signum() > 0)
                    .toList();
            double configuredWeight = relations.stream()
                    .mapToDouble(item -> item.getStrength().doubleValue()).sum();
            double observedWeight = 0D;
            double weightedScore = 0D;
            double weightedConfidence = 0D;
            List<Map<String, Object>> contributions = new ArrayList<>();
            for (AbilityPointCompetencyRelation relation : relations) {
                Map<String, Object> coursePoint = coursePointIndex.get(relation.getAbilityPointId());
                if (coursePoint == null || !(coursePoint.get("score") instanceof Number score)) continue;
                double weight = relation.getStrength().doubleValue();
                observedWeight += weight;
                weightedScore += score.doubleValue() * weight;
                double relationConfidence = relation.getConfidence() == null ? 0D : relation.getConfidence().doubleValue();
                weightedConfidence += relationConfidence * weight;
                Map<String, Object> contribution = new LinkedHashMap<>();
                contribution.put("abilityPointId", relation.getAbilityPointId());
                contribution.put("name", coursePoint.get("name"));
                contribution.put("weight", weight);
                contribution.put("score", score.intValue());
                contribution.put("confidence", relationConfidence);
                contribution.put("strengthSource", relation.getStrengthSource());
                contributions.add(contribution);
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("competencyId", competency.getCompetencyId());
            item.put("name", competency.getName());
            item.put("description", competency.getDescription());
            item.put("score", observedWeight == 0D ? null : Math.round(weightedScore / observedWeight));
            item.put("hasEvidence", observedWeight > 0D);
            item.put("status", observedWeight > 0D ? "projected" : "evidence_insufficient");
            item.put("coverage", contributions.size() + "/" + relations.size());
            item.put("coverageRate", configuredWeight == 0D ? 0D : round(observedWeight / configuredWeight));
            item.put("confidence", observedWeight == 0D ? 0D
                    : round(weightedConfidence / observedWeight
                    * (configuredWeight == 0D ? 0D : observedWeight / configuredWeight)));
            item.put("matrixVersion", mapping.getMatrixVersion());
            item.put("formulaVersion", FORMULA_VERSION);
            item.put("contributions", contributions);
            result.add(item);
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 10000D) / 10000D;
    }
}
