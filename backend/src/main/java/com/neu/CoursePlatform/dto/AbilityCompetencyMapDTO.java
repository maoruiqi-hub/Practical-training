package com.neu.CoursePlatform.dto;

import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.AbilityPointCompetencyRelation;
import com.neu.CoursePlatform.entity.CompetencyPoint;
import com.neu.CoursePlatform.entity.CompetencyTaskObservation;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AbilityCompetencyMapDTO {
    private List<AbilityPoint> abilityPoints;
    private List<CompetencyPoint> competencies;
    private List<AbilityPointCompetencyRelation> relations;
    private List<CompetencyTaskObservation> observations;
    private String matrixVersion;
}
