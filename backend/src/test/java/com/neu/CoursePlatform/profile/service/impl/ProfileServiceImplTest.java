package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.AbilityKnowledgePoint;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.entity.KnowledgeMastery;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.KnowledgePointFloorStatus;
import com.neu.CoursePlatform.mapper.AbilityKnowledgePointMapper;
import com.neu.CoursePlatform.mapper.KnowledgeMasteryMapper;
import com.neu.CoursePlatform.mapper.KnowledgePointFloorStatusMapper;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.StudentProfile;
import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
import com.neu.CoursePlatform.profile.mapper.CompetencyScoreHistoryMapper;
import com.neu.CoursePlatform.profile.mapper.CompetencyScoreMapper;
import com.neu.CoursePlatform.profile.mapper.GrowthHistoryMapper;
import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
import com.neu.CoursePlatform.profile.rule.GrowthRuleEngine;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    @Mock
    private StudentProfileMapper profileMapper;

    @Mock
    private CompetencyScoreMapper competencyMapper;

    @Mock
    private CompetencyScoreHistoryMapper historyMapper;

    @Mock
    private GrowthHistoryMapper growthHistoryMapper;

    @Mock
    private AchievementMapper achievementMapper;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private AbilityPointService abilityPointService;

    @Mock
    private GameEventPublisher eventPublisher;

    @Mock
    private CourseGameConfigService gameConfigService;

    @Mock
    private KnowledgePointFloorStatusMapper floorStatusMapper;

    @Mock
    private KnowledgePointService knowledgePointService;

    @Mock
    private AbilityKnowledgePointMapper abilityKnowledgePointMapper;

    @Mock
    private KnowledgeMasteryMapper knowledgeMasteryMapper;

    @Test
    void profileInitializationDoesNotCreateFixedFiftyAbilityScores() {
        when(profileMapper.selectOne(any())).thenReturn(null);
        ProfileServiceImpl service = new ProfileServiceImpl(
                profileMapper,
                competencyMapper,
                historyMapper,
                growthHistoryMapper,
                achievementMapper,
                studentMapper,
                abilityPointService,
                new GrowthRuleEngine(),
                eventPublisher,
                gameConfigService);

        service.getOrCreateProfile(2024001, 101);

        verify(competencyMapper, never()).insert(any(CompetencyScore.class));
    }

    @Test
    void towerMapUsesKnowledgePointStatusAndMasteryBeforeAbilityScore() {
        StudentProfile profile = new StudentProfile();
        profile.setStudentNo(1);
        profile.setCourseCode(1);
        when(profileMapper.selectOne(any())).thenReturn(profile);

        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setKnowledgePointId("kp-list");
        knowledgePoint.setCourseCode("1");
        knowledgePoint.setName("列表与元组");
        knowledgePoint.setDescription("Python 序列基础");
        when(knowledgePointService.listByCourseCode("1", null)).thenReturn(List.of(knowledgePoint));

        AbilityPoint abilityPoint = new AbilityPoint();
        abilityPoint.setAbilityPointId("ap-data");
        abilityPoint.setCourseCode("1");
        abilityPoint.setName("数据结构基础");
        when(abilityPointService.listByCourseCode("1")).thenReturn(List.of(abilityPoint));

        AbilityKnowledgePoint mapping = new AbilityKnowledgePoint();
        mapping.setAbilityPointId("ap-data");
        mapping.setKnowledgePointId("kp-list");
        when(abilityKnowledgePointMapper.selectList(any())).thenReturn(List.of(mapping));

        CompetencyScore score = new CompetencyScore();
        score.setStudentNo(1);
        score.setCourseCode(1);
        score.setAbilityPointId("ap-data");
        score.setScore(88);
        when(competencyMapper.selectList(any())).thenReturn(List.of(score));

        KnowledgeMastery mastery = new KnowledgeMastery();
        mastery.setStudentNo("1");
        mastery.setCourseCode("1");
        mastery.setKnowledgePointId("kp-list");
        mastery.setMasteryScore(45);
        when(knowledgeMasteryMapper.selectList(any())).thenReturn(List.of(mastery));

        KnowledgePointFloorStatus status = new KnowledgePointFloorStatus();
        status.setStudentId("1");
        status.setCourseId("1");
        status.setKnowledgePointId("kp-list");
        status.setStatus("weak");
        when(floorStatusMapper.selectList(any())).thenReturn(List.of(status));

        ProfileServiceImpl service = new ProfileServiceImpl(
                profileMapper,
                competencyMapper,
                historyMapper,
                growthHistoryMapper,
                achievementMapper,
                studentMapper,
                abilityPointService,
                new GrowthRuleEngine(),
                eventPublisher,
                gameConfigService,
                floorStatusMapper,
                knowledgePointService,
                abilityKnowledgePointMapper,
                knowledgeMasteryMapper);

        List<java.util.Map<String, Object>> floors = service.getTowerMap(1, 1);
        java.util.Map<String, Object> floor = floors.get(0);

        assertEquals("kp-list", floor.get("kpId"));
        assertEquals("kp-list", floor.get("knowledgePointId"));
        assertEquals("weak", floor.get("floorStatus"));
        assertEquals(45, floor.get("masteryRate"));
        assertEquals("knowledge_mastery", floor.get("masterySource"));
        assertEquals("ap-data", floor.get("abilityPointId"));
        assertEquals("数据结构基础", floor.get("abilityPointName"));
    }
}
