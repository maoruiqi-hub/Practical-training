package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.SubmissionAiReview;

public interface SubmissionAiReviewService extends IService<SubmissionAiReview> {

    SubmissionAiReview generateReview(String submissionId);

    /** 由提交事务提交后异步触发；失败时保留 pending_review，不生成假评分。 */
    void requestAutomaticReview(String submissionId);

    SubmissionAiReview getLatestBySubmissionId(String submissionId);
}
