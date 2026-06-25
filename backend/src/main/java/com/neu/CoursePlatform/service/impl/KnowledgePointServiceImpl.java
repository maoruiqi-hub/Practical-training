package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgeRelation;
import com.neu.CoursePlatform.entity.Question;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.CourseResourceMapper;
import com.neu.CoursePlatform.mapper.KnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeRelationMapper;
import com.neu.CoursePlatform.service.KnowledgeMasteryService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.QuestionService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KnowledgePointServiceImpl extends ServiceImpl<KnowledgePointMapper, KnowledgePoint>
        implements KnowledgePointService {

    private final KnowledgeRelationMapper knowledgeRelationMapper;
    private final CourseResourceMapper courseResourceMapper;
    private final AbilityKnowledgePointMapper abilityKnowledgePointMapper;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final QuestionService questionService;

    public KnowledgePointServiceImpl(KnowledgeRelationMapper knowledgeRelationMapper,
                                     CourseResourceMapper courseResourceMapper,
                                     AbilityKnowledgePointMapper abilityKnowledgePointMapper,
                                     @Lazy KnowledgeMasteryService knowledgeMasteryService,
                                     QuestionService questionService) {
        this.knowledgeRelationMapper = knowledgeRelationMapper;
        this.courseResourceMapper = courseResourceMapper;
        this.abilityKnowledgePointMapper = abilityKnowledgePointMapper;
        this.knowledgeMasteryService = knowledgeMasteryService;
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
        knowledgeMasteryService.removeByKnowledgePoint(knowledgePointId);
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
