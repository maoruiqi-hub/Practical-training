package com.neu.CoursePlatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@TableName("course_resource")
public class CourseResource {

    @TableId(value = "resource_id", type = IdType.AUTO)
    private String resourceId;

    private String courseCode;
    private String title;
    private String resourceType;
    private String fileUrl;
    private String previewFileUrl;
    private String previewStatus;
    private String previewError;
    private String originalFilename;
    private String chapter;
    private String knowledgePointId;
    private Long fileSize;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
