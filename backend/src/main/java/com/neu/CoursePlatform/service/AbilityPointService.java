package com.neu.CoursePlatform.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.neu.CoursePlatform.entity.AbilityPoint;

import java.util.List;

public interface AbilityPointService extends IService<AbilityPoint> {
    List<AbilityPoint> listByCourseCode(String courseCode);
}
