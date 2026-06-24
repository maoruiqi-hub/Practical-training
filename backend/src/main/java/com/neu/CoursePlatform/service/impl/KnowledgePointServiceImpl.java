package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.CourseResourceMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.KnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint>
        implements KnowledgePointService {

    private final KnowledgeRelationMapper knowledgeRelationMapper;
    private final CourseResourceMapper courseResourceMapper;
    private final AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    private final KnowledgeMasteryMapper knowledgeMasteryMapper;
    private final QuestionService questionService;

    public KnowledgePointServiceImpl(KnowledgeRelationMapper knowledgeRelationMapper,
                                     CourseResourceMapper courseResourceMapper,
                                     AbilityKnowledgePointMapper abilityKnowledgePointMapper,
                                     KnowledgeMasteryMapper knowledgeMasteryMapper,
                                     QuestionService questionService) {
        this.knowledgeRelationMapper = knowledgeRelationMapper;
        this.courseResourceMapper = courseResourceMapper;
        this.abilityKnowledgePointMapper = abilityKnowledgePointMapper;
        this.knowledgeMasteryMapper = knowledgeMasteryMapper;
        this.questionService = questionService;
    }

    @Override
    public List<KnowledgePoint> listByCourse(String courseCode, String lessonNo, String keyword) {
        LambdaQueryWrapper<KnowledgePoint> query = new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseCode, courseCode)
                .orderByAsc(KnowledgePoint::getChapter)
                .orderByAsc(KnowledgePoint::getLessonNo)
                .orderByAsc(KnowledgePoint::getKnowledgePointId);
        if (lessonNo != null && !lessonNo.isBlank()) {
            query.eq(KnowledgePoint::getLessonNo, lessonNo);
        }
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like(KnowledgePoint::getName, keyword)
                    .or()
                    .like(KnowledgePoint::getDescription, keyword));
        }
        return list(query);
    }

    @Override
    public void removePoint(String pointId) {
        assertNoQuestionReference(pointId);
        long relationCount = knowledgeRelationMapper.selectCount(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getFromKnowledgePointId, pointId)
                .or()
                .eq(KnowledgeRelation::getToKnowledgePointId, pointId));
        if (relationCount > 0) {
            throw new IllegalArgumentException("该知识点已被知识关系引用，请先删除相关关系");
        }
        removeById(pointId);
    }

    @Override
    public List<KnowledgePoint> listByCourseCode(String courseCode, String chapter) {
        LambdaQueryWrapper<KnowledgePoint> query = new LambdaQueryWrapper<KnowledgePoint>()
                .eq(KnowledgePoint::getCourseCode, courseCode)
                .orderByAsc(KnowledgePoint::getChapter)
                .orderByAsc(KnowledgePoint::getKnowledgePointId);
        if (chapter != null && !chapter.isBlank()) {
            query.eq(KnowledgePoint::getChapter, chapter);
        }
        return list(query);
    }

    @Override
    public List<KnowledgePoint> getPrerequisiteChain(String knowledgePointId) {
        KnowledgePoint target = getById(knowledgePointId);
        if (target == null) return List.of();
        Map<String, KnowledgePoint> pointsById = new HashMap<>();
        for (KnowledgePoint point : listByCourseCode(target.getCourseCode(), null)) {
            pointsById.put(point.getKnowledgePointId(), point);
        }
        Map<String, List<String>> directPrerequisites = new HashMap<>();
        List<KnowledgeRelation> relations = knowledgeRelationMapper.selectList(
                new LambdaQueryWrapper<KnowledgeRelation>()
                        .eq(KnowledgeRelation::getCourseCode, target.getCourseCode())
                        .eq(KnowledgeRelation::getRelationType, "prerequisite"));
        for (KnowledgeRelation relation : relations) {
            directPrerequisites.computeIfAbsent(relation.getToKnowledgePointId(), ignored -> new ArrayList<>())
                    .add(relation.getFromKnowledgePointId());
        }
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> prerequisiteIds = new LinkedHashSet<>();
        queue.add(knowledgePointId);
        while (!queue.isEmpty()) {
            for (String prerequisiteId : directPrerequisites.getOrDefault(queue.removeFirst(), List.of())) {
                if (prerequisiteIds.add(prerequisiteId)) queue.addLast(prerequisiteId);
            }
        }
        return prerequisiteIds.stream().map(pointsById::get).filter(java.util.Objects::nonNull).toList();
    }

    @Override
    @Transactional
    public boolean deleteWithDependencies(String knowledgePointId) {
        assertNoQuestionReference(knowledgePointId);
        knowledgeRelationMapper.delete(new LambdaQueryWrapper<KnowledgeRelation>()
                .eq(KnowledgeRelation::getFromKnowledgePointId, knowledgePointId)
                .or()
                .eq(KnowledgeRelation::getToKnowledgePointId, knowledgePointId));
        courseResourceMapper.update(null, new LambdaUpdateWrapper<CourseResource>()
                .eq(CourseResource::getKnowledgePointId, knowledgePointId)
                .set(CourseResource::getKnowledgePointId, null));
        abilityKnowledgePointMapper.delete(new LambdaQueryWrapper<AbilityKnowledgePoint>()
                .eq(AbilityKnowledgePoint::getKnowledgePointId, knowledgePointId));
        knowledgeMasteryMapper.delete(new LambdaQueryWrapper<KnowledgeMastery>()
                .eq(KnowledgeMastery::getKnowledgePointId, knowledgePointId));
        return removeById(knowledgePointId);
    }

    private void assertNoQuestionReference(String knowledgePointId) {
        long questionCount = questionService.count(new QueryWrapper<Question>()
                .eq("knowledge_point_id", knowledgePointId));
        if (questionCount > 0) {
            throw new IllegalArgumentException("该知识点已被题目引用，请先调整相关题目");
        }
    }
}
