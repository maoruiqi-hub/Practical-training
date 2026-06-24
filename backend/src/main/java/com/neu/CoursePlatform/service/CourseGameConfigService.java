package com.neu.CoursePlatform.service;

public interface CourseGameConfigService {
    boolean isEnabled(String courseId);
    boolean updateEnabled(String courseId, boolean enabled);
}
