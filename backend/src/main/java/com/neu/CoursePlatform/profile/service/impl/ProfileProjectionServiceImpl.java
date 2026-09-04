package com.neu.CoursePlatform.profile.service.impl;

import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.entity.LearningAnswerEvidence;
import com.neu.CoursePlatform.entity.StudentTowerAttempt;
import com.neu.CoursePlatform.entity.TaskSubmission;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.mapper.LearningAnswerEvidenceMapper;
import com.neu.CoursePlatform.mapper.StudentTowerAttemptMapper;
import com.neu.CoursePlatform.mapper.TaskSubmissionMapper;
import com.neu.CoursePlatform.profile.entity.ProfileProjectionLedger;
import com.neu.CoursePlatform.profile.event.AchievementEvaluationRequestedEvent;
import com.neu.CoursePlatform.profile.mapper.ProfileProjectionLedgerMapper;
import com.neu.CoursePlatform.profile.service.ProfileProjectionService;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.LearningTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

@Service
public class ProfileProjectionServiceImpl implements ProfileProjectionService {
    private static final Logger log = LoggerFactory.getLogger(ProfileProjectionServiceImpl.class);
    private static final String ANSWER_PROJECTION = "answer_profile_v1";
    private static final String TOWER_ATTEMPT_PROJECTION = "tower_attempt_profile_v1";
    private static final String BOSS_TASK_PROJECTION = "boss_task_profile_v1";

    private final ProfileProjectionLedgerMapper ledgerMapper;
    private final LearningAnswerEvidenceMapper evidenceMapper;
    private final StudentTowerAttemptMapper towerAttemptMapper;
    private final TaskSubmissionMapper submissionMapper;
    private final LearningTaskService taskService;
    private final ProfileService profileService;
    private final ApplicationEventPublisher eventPublisher;
    private final CourseGameConfigService gameConfigService;

    public ProfileProjectionServiceImpl(ProfileProjectionLedgerMapper ledgerMapper,
                                        LearningAnswerEvidenceMapper evidenceMapper,
                                        StudentTowerAttemptMapper towerAttemptMapper,
                                        TaskSubmissionMapper submissionMapper,
                                        LearningTaskService taskService,
                                        ProfileService profileService,
                                        ApplicationEventPublisher eventPublisher,
                                        CourseGameConfigService gameConfigService) {
        this.ledgerMapper = ledgerMapper;
        this.evidenceMapper = evidenceMapper;
        this.towerAttemptMapper = towerAttemptMapper;
        this.submissionMapper = submissionMapper;
        this.taskService = taskService;
        this.profileService = profileService;
        this.eventPublisher = eventPublisher;
        this.gameConfigService = gameConfigService;
    }

    @Override
    @Transactional
    public boolean applyAnswerEvidence(String evidenceId) {
        if (evidenceId == null || evidenceId.isBlank()) return false;
        LearningAnswerEvidence evidence = evidenceMapper.selectById(evidenceId);
        if (evidence == null) return false;
        if (!gameConfigService.isEnabled(evidence.getCourseCode())) return false;
        Integer studentNo = numericId(evidence.getStudentNo());
        Integer courseCode = numericId(evidence.getCourseCode());
        if (studentNo == null || courseCode == null) {
            log.warn("Profile projection skipped because legacy profile IDs must be numeric: student={}, course={}",
                    evidence.getStudentNo(), evidence.getCourseCode());
            return false;
        }

        ProfileProjectionLedger ledger = new ProfileProjectionLedger();
        ledger.setId(SharedIds.newId());
        ledger.setStudentNo(evidence.getStudentNo());
        ledger.setCourseCode(evidence.getCourseCode());
        ledger.setSourceType("answer_evidence");
        ledger.setSourceId(evidence.getEvidenceId());
        ledger.setProjectionType(ANSWER_PROJECTION);
        ledger.setAppliedAt(LocalDateTime.now());
        if (ledgerMapper.insertIfAbsent(ledger) == 0) return false;

        profileService.updateProfileFromEvidence(studentNo, courseCode,
                Boolean.TRUE.equals(evidence.getCorrect()), "answer",
                evidence.getEvidenceId());
        eventPublisher.publishEvent(new AchievementEvaluationRequestedEvent(studentNo, courseCode));
        return true;
    }

    @Override
    @Transactional
    public boolean applyTowerAttempt(String attemptId) {
        if (attemptId == null || attemptId.isBlank()) return false;
        StudentTowerAttempt attempt = towerAttemptMapper.selectById(attemptId);
        if (attempt == null || !gameConfigService.isEnabled(attempt.getCourseCode())) return false;

        GameDelta delta = towerDelta(attempt.getRoomType(), attempt.getResult());
        if (delta == null) return false;
        Integer studentNo = numericId(attempt.getStudentNo());
        Integer courseCode = numericId(attempt.getCourseCode());
        if (studentNo == null || courseCode == null) {
            log.warn("Tower projection skipped because legacy profile IDs must be numeric: student={}, course={}",
                    attempt.getStudentNo(), attempt.getCourseCode());
            return false;
        }

        ProfileProjectionLedger ledger = new ProfileProjectionLedger();
        ledger.setId(SharedIds.newId());
        ledger.setStudentNo(attempt.getStudentNo());
        ledger.setCourseCode(attempt.getCourseCode());
        ledger.setSourceType("tower_attempt");
        ledger.setSourceId(attempt.getAttemptId());
        ledger.setProjectionType(TOWER_ATTEMPT_PROJECTION);
        ledger.setAppliedAt(LocalDateTime.now());
        if (ledgerMapper.insertIfAbsent(ledger) == 0) return false;

        profileService.applyGameDelta(studentNo, courseCode, delta.hp(), delta.atk(), delta.def(),
                delta.exp(), delta.coins(), delta.energy(), "tower_attempt", attempt.getAttemptId());
        eventPublisher.publishEvent(new AchievementEvaluationRequestedEvent(studentNo, courseCode));
        return true;
    }

    private GameDelta towerDelta(String roomType, String result) {
        if (!"battle".equals(roomType) && !"elite".equals(roomType) && !"boss".equals(roomType)) return null;
        if ("failed".equals(result)) return new GameDelta(-5, 0, 0, 10, 0, -1);
        // 诊断跳过战斗不等于完成战斗，不发战斗成长奖励。
        if (!"cleared".equals(result)) return null;
        return switch (roomType == null ? "" : roomType) {
            case "battle" -> new GameDelta(0, 1, 1, 80, 20, 0);
            case "elite" -> new GameDelta(0, 2, 1, 120, 40, 1);
            case "boss" -> new GameDelta(0, 3, 3, 250, 80, 2);
            default -> null;
        };
    }

    @Override
    @Transactional
    public boolean applyBossTaskSubmission(String submissionId) {
        if (submissionId == null || submissionId.isBlank()) return false;
        TaskSubmission submission = submissionMapper.selectById(submissionId);
        if (submission == null || submission.getTaskNo() == null
                || !"graded".equalsIgnoreCase(submission.getStatus())) return false;
        LearningTask task = taskService.getById(submission.getTaskNo());
        if (task == null || !isBossTask(task) || task.getScore() == null || task.getScore() <= 0
                || submission.getScore() == null || submission.getScore() < task.getScore()
                || !gameConfigService.isEnabled(task.getCourseCode())) return false;
        Integer studentNo = numericId(submission.getStudentNo());
        Integer courseCode = numericId(task.getCourseCode());
        if (studentNo == null || courseCode == null) return false;

        ProfileProjectionLedger ledger = new ProfileProjectionLedger();
        ledger.setId(SharedIds.newId());
        ledger.setStudentNo(submission.getStudentNo());
        ledger.setCourseCode(task.getCourseCode());
        ledger.setSourceType("boss_task_submission");
        ledger.setSourceId(submission.getSubmissionId());
        ledger.setProjectionType(BOSS_TASK_PROJECTION);
        ledger.setAppliedAt(LocalDateTime.now());
        if (ledgerMapper.insertIfAbsent(ledger) == 0) return false;

        profileService.applyGameDelta(studentNo, courseCode, 0, 3, 3, 250, 80, 2,
                "boss_task_submission", submission.getSubmissionId());
        eventPublisher.publishEvent(new AchievementEvaluationRequestedEvent(studentNo, courseCode));
        return true;
    }

    private boolean isBossTask(LearningTask task) {
        return "boss".equalsIgnoreCase(task.getTaskType())
                || "boss_exam".equalsIgnoreCase(task.getTaskType());
    }

    private record GameDelta(int hp, int atk, int def, int exp, int coins, int energy) { }

    private Integer numericId(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
