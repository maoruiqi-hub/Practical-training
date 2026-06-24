package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.OfficePreviewResult;
import com.neu.CoursePlatform.service.OfficePreviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class OfficePreviewServiceImpl implements OfficePreviewService {

    private final FileStorageService fileStorageService;
    private final boolean enabled;
    private final String sofficePath;
    private final long timeoutSeconds;

    public OfficePreviewServiceImpl(FileStorageService fileStorageService,
                                    @Value("${course-resource.office-preview.enabled:false}") boolean enabled,
                                    @Value("${course-resource.office-preview.soffice-path:soffice}") String sofficePath,
                                    @Value("${course-resource.office-preview.timeout-seconds:60}") long timeoutSeconds) {
        this.fileStorageService = fileStorageService;
        this.enabled = enabled;
        this.sofficePath = sofficePath;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public OfficePreviewResult generatePreview(String originalFileUrl, String resourceType) {
        if (!"ppt".equals(resourceType) && !"word".equals(resourceType)) return OfficePreviewResult.notRequired();
        if (!enabled) return OfficePreviewResult.unavailable();

        try {
            Path source = fileStorageService.getPrivateFilePath(originalFileUrl);
            Path outputDirectory = source.getParent().resolve("previews");
            Files.createDirectories(outputDirectory);
            Process process = new ProcessBuilder(sofficePath, "--headless", "--convert-to", "pdf",
                    "--outdir", outputDirectory.toString(), source.toString())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return OfficePreviewResult.failed();
            }
            Path previewPath = outputDirectory.resolve(removeExtension(source.getFileName().toString()) + ".pdf");
            if (process.exitValue() != 0 || !Files.isRegularFile(previewPath)) return OfficePreviewResult.failed();
            return OfficePreviewResult.ready(fileStorageService.toPrivateStoredPath(previewPath));
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) Thread.currentThread().interrupt();
            return OfficePreviewResult.failed();
        }
    }

    private String removeExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? filename : filename.substring(0, dotIndex);
    }
}
