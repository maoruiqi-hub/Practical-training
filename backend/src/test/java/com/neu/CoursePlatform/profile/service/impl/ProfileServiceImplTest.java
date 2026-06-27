package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.common.event.GameEventPublisher;
import com.neu.CoursePlatform.entity.AbilityPoint;
import com.neu.CoursePlatform.mapper.StudentMapper;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
import com.neu.CoursePlatform.profile.mapper.CompetencyScoreHistoryMapper;
import com.neu.CoursePlatform.profile.mapper.CompetencyScoreMapper;
import com.neu.CoursePlatform.profile.mapper.GrowthHistoryMapper;
import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
import com.neu.CoursePlatform.profile.rule.GrowthRuleEngine;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    @Test
    void profileInitializationUsesRealAbilityPoints() {
        AbilityPoint abilityPoint = new AbilityPoint();
        abilityPoint.setAbilityPointId("AP-REAL-1");
        abilityPoint.setCourseCode("101");
        abilityPoint.setName("真实能力点");
        abilityPoint.setDescription("来自模块1");
        when(profileMapper.selectOne(any())).thenReturn(null);
        when(abilityPointService.listByCourseCode("101")).thenReturn(List.of(abilityPoint));
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

        ArgumentCaptor<CompetencyScore> captor = ArgumentCaptor.forClass(CompetencyScore.class);
        verify(competencyMapper).insert(captor.capture());
        CompetencyScore saved = captor.getValue();
        assertEquals(2024001, saved.getStudentNo());
        assertEquals(101, saved.getCourseCode());
        assertEquals("AP-REAL-1", saved.getAbilityPointId());
        assertEquals("真实能力点", saved.getAbilityPointName());
        assertEquals(50, saved.getScore());
    }
}
