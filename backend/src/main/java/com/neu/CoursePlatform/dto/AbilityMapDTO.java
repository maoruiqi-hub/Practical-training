package com.neu.CoursePlatform.dto;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor
public class AbilityMapDTO { private List<AbilityPoint> abilityPoints; private List<AbilityKnowledgePoint> mappings; }
