package com.neu.CoursePlatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.entity.CourseGameConfig;
import com.neu.CoursePlatform.mapper.CourseGameConfigMapper;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CourseGameConfigServiceImpl implements CourseGameConfigService {
    private final CourseGameConfigMapper mapper;

    public CourseGameConfigServiceImpl(CourseGameConfigMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean isEnabled(String courseId) {
        CourseGameConfig config = mapper.selectOne(new LambdaQueryWrapper<CourseGameConfig>()
                .eq(CourseGameConfig::getCourseId, courseId));
        return config != null && Boolean.TRUE.equals(config.getGameModeEnabled());
    }

    @Override
    public boolean updateEnabled(String courseId, boolean enabled) {
        CourseGameConfig config = mapper.selectOne(new LambdaQueryWrapper<CourseGameConfig>()
                .eq(CourseGameConfig::getCourseId, courseId));
        LocalDateTime now = LocalDateTime.now();
        if (config == null) {
            config = new CourseGameConfig();
            config.setId(SharedIds.newId());
            config.setCourseId(courseId);
            config.setCreatedAt(now);
            config.setGameModeEnabled(enabled);
            config.setUpdatedAt(now);
            return mapper.insert(config) > 0;
        }
        config.setGameModeEnabled(enabled);
        config.setUpdatedAt(now);
        return mapper.updateById(config) > 0;
    }
}
