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

    @Override
    public String store(MultipartFile file, String directory) throws IOException {
        Path dir = Path.of(directory);
        Files.createDirectories(dir);
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path target = dir.resolve(filename);
        Files.write(target, file.getBytes());
        return target.toString().replace("\\", "/").replace("../", "");
    }
}
