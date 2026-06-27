package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.neu.CoursePlatform.entity.LearningBehaviorLog;
import com.neu.CoursePlatform.mapper.BehaviorLogMapper;
import com.neu.CoursePlatform.service.BehaviorLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class BehaviorLogServiceImpl extends ServiceImpl<BehaviorLogMapper, LearningBehaviorLog> implements BehaviorLogService {

    @Override
    public void record(LearningBehaviorLog log) {
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(LocalDateTime.now());
        }
        if (log.getStartTime() == null) {
            log.setStartTime(LocalDateTime.now());
        }
        save(log);
    }

    @Override
    public List<LearningBehaviorLog> listByUserId(String userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public List<LearningBehaviorLog> listByTaskNo(String taskNo) {
        return baseMapper.selectByTaskNo(taskNo);
    }

    @Override
    public List<LearningBehaviorLog> query(Map<String, String> filters) {
        return baseMapper.selectByFilters(
                filters.get("userId"),
                filters.get("userType"),
                filters.get("actionType"),
                filters.get("resourceType"),
                filters.get("course_id"),
                filters.get("startTime"),
                filters.get("endTime")
        );
    }
}
