package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 模块一新增的课程游戏配置；旧 course 表保持兼容。 */
@Data
@TableName("course_game_config")
public class CourseGameConfig {
    @TableId(value = "id", type = IdType.ASSIGN_UUID)
    private String id;
    private String courseId;
    private Boolean gameModeEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
