package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import com.neu.CoursePlatform.mapper.LearningAnswerEvidenceMapper;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.profile.entity.Achievement;
import com.neu.CoursePlatform.profile.mapper.AchievementMapper;
import com.neu.CoursePlatform.profile.mapper.StudentProfileMapper;
import com.neu.CoursePlatform.profile.rule.BadgeRuleEngine;
import com.neu.CoursePlatform.profile.rule.TierTitleEngine;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.LearningTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncentiveServiceImplTest {

    @Test
    void awardsComboBadgeFromPersistedEvidenceInsteadOfRequestStatistics() {
        AchievementMapper achievementMapper = mock(AchievementMapper.class);
        LearningAnswerEvidenceMapper evidenceMapper = mock(LearningAnswerEvidenceMapper.class);
        TaskSubmissionMapper submissionMapper = mock(TaskSubmissionMapper.class);
        when(achievementMapper.selectList(any())).thenReturn(List.of());
        when(evidenceMapper.selectList(any())).thenReturn(correctEvidence(10));
        when(submissionMapper.selectByStudentNoAndCourse("1", "101")).thenReturn(List.of());
        IncentiveServiceImpl service = new IncentiveServiceImpl(
                achievementMapper, mock(StudentProfileMapper.class), mock(ProfileService.class),
                new BadgeRuleEngine(), new TierTitleEngine(), evidenceMapper, submissionMapper,
                mock(LearningTaskService.class));

        List<Achievement> awarded = service.evaluateAndAward(1, 101);

        assertEquals(1, awarded.size());
        ArgumentCaptor<Achievement> captor = ArgumentCaptor.forClass(Achievement.class);
        verify(achievementMapper).insert(captor.capture());
        assertEquals("combo_10", captor.getValue().getBadgeCode());
        assertEquals("连击王", captor.getValue().getName());
        assertTrue(captor.getValue().getMetadata().contains("\"source\":\"trusted_facts_v1\""));
        assertTrue(captor.getValue().getMetadata().contains("\"consecutiveCorrect\":10"));
    }

    private List<LearningAnswerEvidence> correctEvidence(int count) {
        List<LearningAnswerEvidence> evidence = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LearningAnswerEvidence item = new LearningAnswerEvidence();
            item.setQuestionId("q-" + i);
            item.setCorrect(true);
            item.setAnsweredAt(LocalDateTime.of(2026, 9, 2, 12, i));
            evidence.add(item);
        }
        return evidence;
    }
}
