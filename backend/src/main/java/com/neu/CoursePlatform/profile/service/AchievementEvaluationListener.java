package com.neu.CoursePlatform.profile.service;

import com.neu.CoursePlatform.profile.event.AchievementEvaluationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AchievementEvaluationListener {
    private static final Logger log = LoggerFactory.getLogger(AchievementEvaluationListener.class);

    private final IncentiveService incentiveService;

    public AchievementEvaluationListener(IncentiveService incentiveService) {
        this.incentiveService = incentiveService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void evaluate(AchievementEvaluationRequestedEvent event) {
        try {
            incentiveService.evaluateAndAward(event.studentNo(), event.courseCode());
        } catch (RuntimeException e) {
            log.warn("Achievement evaluation failed for student={}, course={}",
                    event.studentNo(), event.courseCode(), e);
        }
    }
}
