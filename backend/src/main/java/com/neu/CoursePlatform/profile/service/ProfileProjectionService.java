package com.neu.CoursePlatform.profile.service;

public interface ProfileProjectionService {
    boolean applyAnswerEvidence(String evidenceId);

    /**
     * 将服务端已经落库的爬塔战斗结果投影到画像。返回 false 表示记录不存在、
     * 不属于可奖励的战斗，或该尝试已经投影过。
     */
    boolean applyTowerAttempt(String attemptId);

    /** 按已批改且满分的 Boss 任务提交事实发放一次性完成奖励。 */
    boolean applyBossTaskSubmission(String submissionId);
}
