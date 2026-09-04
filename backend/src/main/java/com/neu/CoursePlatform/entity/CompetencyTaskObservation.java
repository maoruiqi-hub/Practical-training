package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CompetencyTaskObservation {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String courseCode;
    private String taskNo;
    private String competencyId;
    private String status;
    private LocalDateTime updatedAt;
}
