package com.neu.CoursePlatform.service;

public interface CourseGameConfigService {
    boolean isGameModeEnabled(String courseCode);
    void setGameModeEnabled(String courseCode, boolean enabled);
}
