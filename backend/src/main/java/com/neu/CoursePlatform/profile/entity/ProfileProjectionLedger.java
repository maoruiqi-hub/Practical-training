package com.neu.CoursePlatform.profile.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("profile_projection_ledger")
public class ProfileProjectionLedger {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String studentNo;
    private String courseCode;
    private String sourceType;
    private String sourceId;
    private String projectionType;
    private LocalDateTime appliedAt;
}
