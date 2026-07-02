package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.dto.AbilityMapDTO;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.service.AbilityMapService;
import com.neu.CoursePlatform.service.AbilityPointService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AbilityMapServiceImpl implements AbilityMapService {
    private final AbilityPointService abilityPointService;
    private final AbilityKnowledgePointMapper mappingMapper;
    public AbilityMapServiceImpl(AbilityPointService abilityPointService, AbilityKnowledgePointMapper mappingMapper) { this.abilityPointService = abilityPointService; this.mappingMapper = mappingMapper; }
    @Override public AbilityMapDTO getByCourseCode(String courseCode) { List<AbilityPoint> points=abilityPointService.listByCourseCode(courseCode); if(points.isEmpty()) return new AbilityMapDTO(points,List.of()); return new AbilityMapDTO(points,mappingMapper.selectList(new LambdaQueryWrapper<AbilityKnowledgePoint>().in(AbilityKnowledgePoint::getAbilityPointId,points.stream().map(AbilityPoint::getAbilityPointId).toList()))); }
    @Override @Transactional public boolean bindKnowledgePoint(String abilityPointId, String knowledgePointId) { long count=mappingMapper.selectCount(new LambdaQueryWrapper<AbilityKnowledgePoint>().eq(AbilityKnowledgePoint::getAbilityPointId,abilityPointId).eq(AbilityKnowledgePoint::getKnowledgePointId,knowledgePointId)); if(count>0) return false; AbilityKnowledgePoint mapping=new AbilityKnowledgePoint(); mapping.setId(UUID.randomUUID().toString()); mapping.setAbilityPointId(abilityPointId); mapping.setKnowledgePointId(knowledgePointId); mappingMapper.insertWithId(mapping); return true; }
    @Override public boolean unbindKnowledgePoint(String abilityPointId, String knowledgePointId) { return mappingMapper.delete(new LambdaQueryWrapper<AbilityKnowledgePoint>().eq(AbilityKnowledgePoint::getAbilityPointId, abilityPointId).eq(AbilityKnowledgePoint::getKnowledgePointId, knowledgePointId)) > 0; }
    @Override @Transactional public boolean deleteAbilityPoint(String abilityPointId) { mappingMapper.delete(new LambdaQueryWrapper<AbilityKnowledgePoint>().eq(AbilityKnowledgePoint::getAbilityPointId, abilityPointId)); return abilityPointService.removeById(abilityPointId); }
}
