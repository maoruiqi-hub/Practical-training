package com.neu.CoursePlatform.dto;

import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KnowledgeGraphDTO {
    private List<KnowledgePoint> nodes;
    private List<KnowledgeRelation> edges;
}
