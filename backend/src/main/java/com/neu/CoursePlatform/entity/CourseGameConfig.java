package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** Module 1 owns the per-course switch that enables tower-game integration. */
@Data
@TableName("course_game_config")
public class CourseGameConfig {

    @TableId(value = "course_code", type = IdType.INPUT)
    private String courseCode;

    private Boolean gameModeEnabled;
    private LocalDateTime updatedAt;
}
