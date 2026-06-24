package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.SubmissionAiReview;

public interface SubmissionAiReviewService extends IService<SubmissionAiReview> {

    SubmissionAiReview generateReview(String submissionId);

    SubmissionAiReview getLatestBySubmissionId(String submissionId);
}
