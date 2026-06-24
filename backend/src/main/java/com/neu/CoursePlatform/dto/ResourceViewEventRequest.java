package com.neu.CoursePlatform.dto;

import lombok.Data;

@Data
public class ResourceViewEventRequest {
    private String action;
    private Long durationMs;
}
