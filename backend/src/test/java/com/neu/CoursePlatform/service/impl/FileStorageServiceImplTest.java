package com.neu.CoursePlatform.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceImplTest {

    @Test
    void promotesTemporaryUploadAndCleansItIdempotently() throws Exception {
        FileStorageServiceImpl storage = new FileStorageServiceImpl();
        String temporaryPath = storage.storeTemporary(
                new MockMultipartFile("file", "report.txt", "text/plain", "report".getBytes()),
                "../resource/HomeworkUpload/");
        String finalPath = null;
        try {
            Path temporaryFile = Path.of("..").resolve(temporaryPath).toAbsolutePath().normalize();
            assertTrue(Files.isRegularFile(temporaryFile));

            finalPath = storage.promoteTemporary(temporaryPath, "../resource/HomeworkUpload/");
            Path finalFile = Path.of("..").resolve(finalPath).toAbsolutePath().normalize();
            assertTrue(Files.isRegularFile(finalFile));
            assertFalse(Files.exists(temporaryFile));
            assertEquals("report", Files.readString(finalFile));

            storage.deleteStoredFileIfExists(finalPath);
            storage.deleteStoredFileIfExists(finalPath);
            assertFalse(Files.exists(finalFile));
        } finally {
            storage.deleteStoredFileIfExists(temporaryPath);
            storage.deleteStoredFileIfExists(finalPath);
        }
    }
}
