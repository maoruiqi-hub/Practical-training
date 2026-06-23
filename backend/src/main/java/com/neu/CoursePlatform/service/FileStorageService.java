package com.neu.CoursePlatform.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {

    String store(MultipartFile file, String directory) throws IOException;
}
