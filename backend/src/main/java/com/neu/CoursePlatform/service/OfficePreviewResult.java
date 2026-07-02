package com.neu.CoursePlatform.service;

public record OfficePreviewResult(String status, String previewFileUrl, String errorMessage) {

    public static OfficePreviewResult notRequired() {
        return new OfficePreviewResult("not_required", null, null);
    }

    public static OfficePreviewResult unavailable() {
        return new OfficePreviewResult("unavailable", null, "Office 预览转换服务未配置");
    }

    public static OfficePreviewResult failed() {
        return new OfficePreviewResult("failed", null, "Office 文件转换失败，请下载原文件查看");
    }

    public static OfficePreviewResult ready(String previewFileUrl) {
        return new OfficePreviewResult("ready", previewFileUrl, null);
    }
}
