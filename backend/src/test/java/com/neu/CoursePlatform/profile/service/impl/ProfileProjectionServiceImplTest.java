package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.mapper.LearningAnswerEvidenceMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.profile.mapper.ProfileProjectionLedgerMapper;
import com.neu.CoursePlatform.profile.event.AchievementEvaluationRequestedEvent;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.LearningTaskService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileProjectionServiceImplTest {

    @Test
    void appliesNewEvidenceThroughExistingProfileRulesAndEvaluatesBadges() {
        ProfileProjectionLedgerMapper ledgerMapper = mock(ProfileProjectionLedgerMapper.class);
        LearningAnswerEvidenceMapper evidenceMapper = mock(LearningAnswerEvidenceMapper.class);
        ProfileService profileService = mock(ProfileService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CourseGameConfigService gameConfigService = mock(CourseGameConfigService.class);
        when(gameConfigService.isEnabled("101")).thenReturn(true);
        when(ledgerMapper.insertIfAbsent(any())).thenReturn(1);
        when(evidenceMapper.selectById("e-1")).thenReturn(evidence("e-1", true, "quiz"));
        ProfileProjectionServiceImpl service = new ProfileProjectionServiceImpl(
                ledgerMapper, evidenceMapper, mock(StudentTowerAttemptMapper.class),
                mock(TaskSubmissionMapper.class), mock(LearningTaskService.class), profileService,
                eventPublisher, gameConfigService);

        assertTrue(service.applyAnswerEvidence("e-1"));
        verify(profileService).updateProfileFromEvidence(1, 101, true, "answer", "e-1");
        ArgumentCaptor<AchievementEvaluationRequestedEvent> event =
                ArgumentCaptor.forClass(AchievementEvaluationRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertTrue(event.getValue().studentNo() == 1 && event.getValue().courseCode() == 101);
    }

    @Test
    void replayedEvidenceDoesNotChangeProfileOrBadgesAgain() {
        ProfileProjectionLedgerMapper ledgerMapper = mock(ProfileProjectionLedgerMapper.class);
        LearningAnswerEvidenceMapper evidenceMapper = mock(LearningAnswerEvidenceMapper.class);
        ProfileService profileService = mock(ProfileService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CourseGameConfigService gameConfigService = mock(CourseGameConfigService.class);
        when(gameConfigService.isEnabled("101")).thenReturn(true);
        when(ledgerMapper.insertIfAbsent(any())).thenReturn(0);
        when(evidenceMapper.selectById("e-1")).thenReturn(evidence("e-1", true, "quiz"));
        ProfileProjectionServiceImpl service = new ProfileProjectionServiceImpl(
                ledgerMapper, evidenceMapper, mock(StudentTowerAttemptMapper.class),
                mock(TaskSubmissionMapper.class), mock(LearningTaskService.class), profileService,
                eventPublisher, gameConfigService);

        assertFalse(service.applyAnswerEvidence("e-1"));
        verify(profileService, never()).updateProfileFromEvidence(any(), any(), any(Boolean.class), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void appliesOnlyPersistedTowerAttemptAndUsesAttemptAsAuditSource() {
        ProfileProjectionLedgerMapper ledgerMapper = mock(ProfileProjectionLedgerMapper.class);
        LearningAnswerEvidenceMapper evidenceMapper = mock(LearningAnswerEvidenceMapper.class);
        StudentTowerAttemptMapper attemptMapper = mock(StudentTowerAttemptMapper.class);
        ProfileService profileService = mock(ProfileService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CourseGameConfigService gameConfigService = mock(CourseGameConfigService.class);
        when(gameConfigService.isEnabled("101")).thenReturn(true);
        when(ledgerMapper.insertIfAbsent(any())).thenReturn(1);
        when(attemptMapper.selectById("attempt-1")).thenReturn(attempt("attempt-1", "elite", "cleared"));
        ProfileProjectionServiceImpl service = new ProfileProjectionServiceImpl(
                ledgerMapper, evidenceMapper, attemptMapper, mock(TaskSubmissionMapper.class),
                mock(LearningTaskService.class), profileService, eventPublisher, gameConfigService);

        assertTrue(service.applyTowerAttempt("attempt-1"));
        verify(profileService).applyGameDelta(1, 101, 0, 2, 1, 120, 40, 1,
                "tower_attempt", "attempt-1");
        verify(eventPublisher).publishEvent(any(AchievementEvaluationRequestedEvent.class));
    }

    @Test
    void rejectsGenericOrDiagnosisSourceAndReplay() {
        ProfileProjectionLedgerMapper ledgerMapper = mock(ProfileProjectionLedgerMapper.class);
        LearningAnswerEvidenceMapper evidenceMapper = mock(LearningAnswerEvidenceMapper.class);
        StudentTowerAttemptMapper attemptMapper = mock(StudentTowerAttemptMapper.class);
        ProfileService profileService = mock(ProfileService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CourseGameConfigService gameConfigService = mock(CourseGameConfigService.class);
        when(gameConfigService.isEnabled("101")).thenReturn(true);
        when(attemptMapper.selectById("task-submission")).thenReturn(null);
        when(attemptMapper.selectById("diagnosis-1"))
                .thenReturn(attempt("diagnosis-1", "elite", "diagnosis_perfect"));
        when(attemptMapper.selectById("attempt-1")).thenReturn(attempt("attempt-1", "battle", "cleared"));
        when(ledgerMapper.insertIfAbsent(any())).thenReturn(0);
        ProfileProjectionServiceImpl service = new ProfileProjectionServiceImpl(
                ledgerMapper, evidenceMapper, attemptMapper, mock(TaskSubmissionMapper.class),
                mock(LearningTaskService.class), profileService, eventPublisher, gameConfigService);

        assertFalse(service.applyTowerAttempt("task-submission"));
        assertFalse(service.applyTowerAttempt("diagnosis-1"));
        assertFalse(service.applyTowerAttempt("attempt-1"));
        verify(profileService, never()).applyGameDelta(any(), any(), any(Integer.class), any(Integer.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class), any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void appliesFullScoreBossTaskFromPersistedSubmissionExactlyOnce() {
        ProfileProjectionLedgerMapper ledgerMapper = mock(ProfileProjectionLedgerMapper.class);
        TaskSubmissionMapper submissionMapper = mock(TaskSubmissionMapper.class);
        LearningTaskService taskService = mock(LearningTaskService.class);
        ProfileService profileService = mock(ProfileService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        CourseGameConfigService gameConfigService = mock(CourseGameConfigService.class);
        TaskSubmission submission = new TaskSubmission();
        submission.setSubmissionId("submission-1");
        submission.setTaskNo("task-1");
        submission.setStudentNo("1");
        submission.setStatus("graded");
        submission.setScore(100);
        LearningTask task = new LearningTask();
        task.setTaskNo("task-1");
        task.setCourseCode("101");
        task.setTaskType("boss_exam");
        task.setScore(100);
        when(submissionMapper.selectById("submission-1")).thenReturn(submission);
        when(taskService.getById("task-1")).thenReturn(task);
        when(gameConfigService.isEnabled("101")).thenReturn(true);
        when(ledgerMapper.insertIfAbsent(any())).thenReturn(1, 0);
        ProfileProjectionServiceImpl service = new ProfileProjectionServiceImpl(
                ledgerMapper, mock(LearningAnswerEvidenceMapper.class), mock(StudentTowerAttemptMapper.class),
                submissionMapper, taskService, profileService, eventPublisher, gameConfigService);

        assertTrue(service.applyBossTaskSubmission("submission-1"));
        assertFalse(service.applyBossTaskSubmission("submission-1"));
        verify(profileService).applyGameDelta(1, 101, 0, 3, 3, 250, 80, 2,
                "boss_task_submission", "submission-1");
    }

    private LearningAnswerEvidence evidence(String id, boolean correct, String sourceType) {
        LearningAnswerEvidence evidence = new LearningAnswerEvidence();
        evidence.setEvidenceId(id);
        evidence.setStudentNo("1");
        evidence.setCourseCode("101");
        evidence.setCorrect(correct);
        evidence.setSourceType(sourceType);
        return evidence;
    }

    private StudentTowerAttempt attempt(String id, String roomType, String result) {
        StudentTowerAttempt attempt = new StudentTowerAttempt();
        attempt.setAttemptId(id);
        attempt.setStudentNo("1");
        attempt.setCourseCode("101");
        attempt.setRoomType(roomType);
        attempt.setResult(result);
        return attempt;
    }
}
