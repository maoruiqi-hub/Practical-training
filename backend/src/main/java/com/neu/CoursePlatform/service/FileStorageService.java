package com.neu.CoursePlatform.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    String store(MultipartFile file, String directory) throws IOException;

    /** Stores an upload below the temporary upload area and returns its stored path. */
    String storeTemporary(MultipartFile file, String directory) throws IOException;

    /** Atomically moves one temporary upload into its final directory. */
    String promoteTemporary(String temporaryStoredPath, String finalDirectory) throws IOException;

    byte[] readPrivateFile(String storedPath) throws IOException;

    String getPrivateFileContentType(String storedPath) throws IOException;

    Path getPrivateFilePath(String storedPath) throws IOException;

    String toPrivateStoredPath(Path path) throws IOException;

    /** Deletes one file from the private resource root. Missing files are ignored. */
    void deletePrivateFileIfExists(String storedPath) throws IOException;

    /** Deletes one stored resource from the shared resource root. Missing files are ignored. */
    void deleteStoredFileIfExists(String storedPath) throws IOException;
}
