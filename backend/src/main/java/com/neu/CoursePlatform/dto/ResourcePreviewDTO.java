package com.neu.CoursePlatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResourcePreviewDTO {
    private String previewType;
    private String previewUrl;
    private String downloadUrl;
    private boolean previewAvailable;
}
