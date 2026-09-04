package com.neu.CoursePlatform.common.event;

/**
 * 学生提交编程任务后，请求异步生成 AI 评价。
 * 事件只携带提交编号，题目和学生信息由后端重新读取，避免信任客户端数据。
 */
public record SubmissionAssessmentRequestedEvent(String submissionId) {
}
