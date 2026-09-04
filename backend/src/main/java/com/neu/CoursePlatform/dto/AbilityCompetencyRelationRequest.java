package com.neu.CoursePlatform.dto;

import lombok.Data;

@Data
public class AbilityCompetencyRelationRequest {
    private String courseCode;
    private String abilityPointId;
    private String competencyId;
    private String relationStatus;
    private String reviewNote;
}
