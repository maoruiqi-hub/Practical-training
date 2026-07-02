package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Path PRIVATE_RESOURCE_ROOT = Path.of("../private-resource").toAbsolutePath().normalize();

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        Path dir = Path.of(directory);
        Files.createDirectories(dir);
        String originalFilename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String filename = UUID.randomUUID() + "_" + Path.of(originalFilename).getFileName();
        Path target = dir.resolve(filename);
        Files.write(target, file.getBytes());
        return target.toString().replace("\\", "/").replace("../", "");
    }

    @Override
    public byte[] readPrivateFile(String storedPath) throws IOException {
        return Files.readAllBytes(resolvePrivateFile(storedPath));
    }

    @Override
    public String getPrivateFileContentType(String storedPath) throws IOException {
        String contentType = Files.probeContentType(resolvePrivateFile(storedPath));
        return contentType == null ? "application/octet-stream" : contentType;
    }

    @Override
    public Path getPrivateFilePath(String storedPath) throws IOException {
        return resolvePrivateFile(storedPath);
    }

    @Override
    public String toPrivateStoredPath(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(PRIVATE_RESOURCE_ROOT)) throw new IOException("Path is outside private resources");
        return "private-resource/" + PRIVATE_RESOURCE_ROOT.relativize(normalized).toString().replace("\\", "/");
    }

    @Override
    public void deletePrivateFileIfExists(String storedPath) throws IOException {
        if (storedPath == null || storedPath.isBlank()) return;
        Path resolved = Path.of("..").resolve(storedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(PRIVATE_RESOURCE_ROOT)) {
            throw new IOException("Path is outside private resources");
        }
        Files.deleteIfExists(resolved);
    }

    private Path resolvePrivateFile(String storedPath) throws IOException {
        Path resolved = Path.of("..").resolve(storedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(PRIVATE_RESOURCE_ROOT) || !Files.isRegularFile(resolved)) {
            throw new IOException("Private resource does not exist");
        }
        return resolved;
    }
}
