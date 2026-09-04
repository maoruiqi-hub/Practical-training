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

    private static final Path RESOURCE_ROOT = Path.of("..").toAbsolutePath().normalize();
    private static final Path PRIVATE_RESOURCE_ROOT = Path.of("../private-resource").toAbsolutePath().normalize();

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        Path dir = resolveStorageDirectory(directory);
        Files.createDirectories(dir);
        String originalFilename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String filename = UUID.randomUUID() + "_" + Path.of(originalFilename).getFileName();
        Path target = dir.resolve(filename);
        Files.write(target, file.getBytes());
        return toStoredPath(target);
    }

    @Override
    public String storeTemporary(MultipartFile file, String directory) throws IOException {
        Path finalDirectory = resolveStorageDirectory(directory);
        Path temporaryDirectory = finalDirectory.resolveSibling(".upload-tmp").resolve(finalDirectory.getFileName());
        Files.createDirectories(temporaryDirectory);
        String originalFilename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String filename = UUID.randomUUID() + "_" + Path.of(originalFilename).getFileName();
        Path target = temporaryDirectory.resolve(filename);
        Files.write(target, file.getBytes());
        return toStoredPath(target);
    }

    @Override
    public String promoteTemporary(String temporaryStoredPath, String finalDirectory) throws IOException {
        Path source = resolveStoredFile(temporaryStoredPath);
        Path destinationDirectory = resolveStorageDirectory(finalDirectory);
        Files.createDirectories(destinationDirectory);
        Path destination = destinationDirectory.resolve(source.getFileName().toString()).normalize();
        if (!destination.startsWith(RESOURCE_ROOT)) throw new IOException("Path is outside resources");
        try {
            Files.move(source, destination, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(source, destination);
        }
        return toStoredPath(destination);
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

    @Override
    public void deleteStoredFileIfExists(String storedPath) throws IOException {
        if (storedPath == null || storedPath.isBlank()) return;
        Path resolved = resolveStoredFilePath(storedPath);
        Files.deleteIfExists(resolved);
    }

    private Path resolvePrivateFile(String storedPath) throws IOException {
        Path resolved = Path.of("..").resolve(storedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(PRIVATE_RESOURCE_ROOT) || !Files.isRegularFile(resolved)) {
            throw new IOException("Private resource does not exist");
        }
        return resolved;
    }

    private Path resolveStorageDirectory(String directory) throws IOException {
        Path resolved = Path.of(directory).toAbsolutePath().normalize();
        boolean publicResource = resolved.startsWith(RESOURCE_ROOT.resolve("resource"));
        boolean privateResource = resolved.startsWith(RESOURCE_ROOT.resolve("private-resource"));
        if (!publicResource && !privateResource) {
            throw new IOException("Path is outside public resources");
        }
        return resolved;
    }

    private Path resolveStoredFile(String storedPath) throws IOException {
        Path resolved = resolveStoredFilePath(storedPath);
        if (!Files.isRegularFile(resolved)) throw new IOException("Stored resource does not exist");
        return resolved;
    }

    private Path resolveStoredFilePath(String storedPath) throws IOException {
        Path resolved = Path.of("..").resolve(storedPath).toAbsolutePath().normalize();
        if (!resolved.startsWith(RESOURCE_ROOT)) {
            throw new IOException("Path is outside resources or does not exist");
        }
        return resolved;
    }

    private String toStoredPath(Path path) throws IOException {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(RESOURCE_ROOT)) throw new IOException("Path is outside resources");
        return RESOURCE_ROOT.relativize(normalized).toString().replace("\\", "/");
    }
}
