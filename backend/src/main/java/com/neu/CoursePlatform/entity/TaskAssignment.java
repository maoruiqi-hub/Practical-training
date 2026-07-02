package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("task_assignment")
public class TaskAssignment {
    @TableId(value = "assignment_id", type = IdType.AUTO)
    private String assignmentId;

    private String taskNo;
    private String courseCode;
    private String studentNo;
    private String assignedBy;
    private LocalDateTime assignedAt;
    /** assigned/submitted/completed/cancelled */
    private String status;
    private String note;
}
