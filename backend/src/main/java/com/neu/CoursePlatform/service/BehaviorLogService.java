package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;

import java.util.List;
import java.util.Map;

public interface BehaviorLogService extends IService<LearningBehaviorLog> {

    /** 记录一条行为日志 */
    void record(LearningBehaviorLog log);

    /** 按用户查询 */
    List<LearningBehaviorLog> listByUserId(String userId);

    List<LearningBehaviorLog> listRecentByUserId(String userId, int limit);

    /** 按任务查询 */
    List<LearningBehaviorLog> listByTaskNo(String taskNo);

    /** 多条件筛选查询 */
    List<LearningBehaviorLog> query(Map<String, String> filters);

    long sumDurationByUserId(String userId);
}
