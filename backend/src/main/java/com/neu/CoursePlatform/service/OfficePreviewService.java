package com.neu.CoursePlatform.service;

public interface OfficePreviewService {

    OfficePreviewResult generatePreview(String originalFileUrl, String resourceType);
}
