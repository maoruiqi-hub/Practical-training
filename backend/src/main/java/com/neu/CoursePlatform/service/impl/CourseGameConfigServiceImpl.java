package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.entity.CourseGameConfig;
import com.neu.CoursePlatform.mapper.CourseGameConfigMapper;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CourseGameConfigServiceImpl implements CourseGameConfigService {

    private final CourseGameConfigMapper configMapper;

    public CourseGameConfigServiceImpl(CourseGameConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    @Override
    public boolean isGameModeEnabled(String courseCode) {
        CourseGameConfig config = configMapper.selectById(courseCode);
        return config != null && Boolean.TRUE.equals(config.getGameModeEnabled());
    }

    @Override
    public void setGameModeEnabled(String courseCode, boolean enabled) {
        CourseGameConfig config = configMapper.selectById(courseCode);
        if (config == null) {
            config = new CourseGameConfig();
            config.setCourseCode(courseCode);
            config.setGameModeEnabled(enabled);
            config.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(config);
            return;
        }
        config.setGameModeEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());
        configMapper.updateById(config);
    }
}
