package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.common.event.ResourceViewEvent;
import com.neu.CoursePlatform.dto.ResourcePreviewDTO;
import com.neu.CoursePlatform.dto.ResourceViewEventRequest;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.OfficePreviewResult;
import com.neu.CoursePlatform.service.OfficePreviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@RestController
@RequestMapping("/api/resources")
public class CourseResourceController {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> PREVIEW_TYPES = Set.of("pdf", "video", "image", "text");

    private final CourseResourceService courseResourceService;
    private final CourseService courseService;
    private final KnowledgePointService knowledgePointService;
    private final FileStorageService fileStorageService;
    private final Auth auth;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final OfficePreviewService officePreviewService;

    public CourseResourceController(CourseResourceService courseResourceService,
                                    CourseService courseService,
                                    KnowledgePointService knowledgePointService,
                                    FileStorageService fileStorageService,
                                    Auth auth,
                                    ApplicationEventPublisher applicationEventPublisher,
                                    OfficePreviewService officePreviewService) {
        this.courseResourceService = courseResourceService;
        this.courseService = courseService;
        this.knowledgePointService = knowledgePointService;
        this.fileStorageService = fileStorageService;
        this.auth = auth;
        this.applicationEventPublisher = applicationEventPublisher;
        this.officePreviewService = officePreviewService;
    }

    @GetMapping
    public Result<?> list(@RequestParam String courseCode,
                          @RequestParam(required = false) String chapter,
                          @RequestParam(required = false) String knowledgePointId,
                          @RequestParam(required = false) String resourceType,
                          HttpSession session) {
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(courseResourceService.listByFilters(courseCode, chapter, knowledgePointId, resourceType));
    }

    @GetMapping("/{resourceId}")
    public Result<CourseResource> detail(@PathVariable String resourceId, HttpSession session) {
        CourseResource resource = courseResourceService.getById(resourceId);
        return resource == null ? Result.fail("课程资源不存在") : Result.ok(resource);
    }

    @GetMapping("/{resourceId}/preview")
    public Result<ResourcePreviewDTO> preview(@PathVariable String resourceId, HttpSession session) {
        CourseResource resource = courseResourceService.getById(resourceId);
        if (resource == null) return Result.fail("课程资源不存在");
        String contentUrl = "/course-resource/" + resourceId + "/content";
        boolean officePreviewReady = ("ppt".equals(resource.getResourceType()) || "word".equals(resource.getResourceType()))
                && "ready".equals(resource.getPreviewStatus());
        String previewUrl = officePreviewReady ? contentUrl + "?preview=true" : contentUrl;
        String previewType = officePreviewReady ? "pdf" : resource.getResourceType();
        return Result.ok(new ResourcePreviewDTO(previewType, previewUrl, contentUrl + "?download=true",
                officePreviewReady || PREVIEW_TYPES.contains(resource.getResourceType())));
    }

    @GetMapping("/{resourceId}/content")
    public ResponseEntity<byte[]> content(@PathVariable String resourceId,
                                          @RequestParam(defaultValue = "false") boolean download,
                                          @RequestParam(defaultValue = "false") boolean preview,
                                          HttpSession session) {
        CourseResource resource = courseResourceService.getById(resourceId);
        if (resource == null) return ResponseEntity.notFound().build();
        if (preview && (resource.getPreviewFileUrl() == null || !"ready".equals(resource.getPreviewStatus()))) {
            return ResponseEntity.notFound().build();
        }
        try {
            String fileUrl = preview ? resource.getPreviewFileUrl() : resource.getFileUrl();
            byte[] content = fileStorageService.readPrivateFile(fileUrl);
            MediaType mediaType = MediaType.parseMediaType(fileStorageService.getPrivateFileContentType(fileUrl));
            ContentDisposition disposition = ContentDisposition.builder(download ? "attachment" : "inline")
                    .filename(resource.getOriginalFilename(), StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(content.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(content);
        } catch (IOException | IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{resourceId}/view-events")
    public Result<Void> recordViewEvent(@PathVariable String resourceId,
                                        @RequestBody ResourceViewEventRequest request,
                                        HttpSession session) {
        Student student = auth.getStudent(session);
        if (student == null) return Result.fail("仅学生可记录资源学习行为");
        CourseResource resource = courseResourceService.getById(resourceId);
        if (resource == null) return Result.fail("课程资源不存在");
        if (request == null || request.getAction() == null
                || !("start".equals(request.getAction()) || "end".equals(request.getAction()))) {
            return Result.fail("浏览事件类型必须是 start 或 end");
        }
        if (request.getDurationMs() != null && request.getDurationMs() < 0) return Result.fail("浏览时长不能为负数");
        applicationEventPublisher.publishEvent(new ResourceViewEvent(student.getStudentNo(), resource.getCourseCode(),
                resourceId, resource.getKnowledgePointId(), request.getAction(), request.getDurationMs(), LocalDateTime.now()));
        return Result.ok();
    }

    @PostMapping
    public Result<String> upload(@RequestParam String courseCode,
                                 @RequestParam(required = false) String title,
                                 @RequestParam(required = false) String chapter,
                                 @RequestParam(required = false) String knowledgePointId,
                                 @RequestParam(required = false) String resourceType,
                                 @RequestParam MultipartFile file,
                                 HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        String validationMessage = validateFile(file, resourceType);
        if (validationMessage != null) return Result.fail(validationMessage);
        String knowledgePointMessage = validateKnowledgePoint(courseCode, knowledgePointId);
        if (knowledgePointMessage != null) return Result.fail(knowledgePointMessage);

        String detectedType = detectResourceType(file.getOriginalFilename());
        try {
            CourseResource resource = new CourseResource();
            resource.setCourseCode(courseCode);
            resource.setTitle(title == null || title.isBlank() ? file.getOriginalFilename() : title.trim());
            resource.setResourceType(detectedType);
            resource.setFileUrl(fileStorageService.store(file, "../private-resource/CourseResource/"));
            OfficePreviewResult previewResult = officePreviewService.generatePreview(resource.getFileUrl(), detectedType);
            resource.setPreviewFileUrl(previewResult.previewFileUrl());
            resource.setPreviewStatus(previewResult.status());
            resource.setPreviewError(previewResult.errorMessage());
            resource.setOriginalFilename(file.getOriginalFilename());
            resource.setChapter(chapter);
            resource.setKnowledgePointId(knowledgePointId);
            resource.setFileSize(file.getSize());
            Teacher teacher = auth.getTeacher(session);
            resource.setUploadedBy(teacher == null ? null : teacher.getTeacherNo());
            resource.setUploadedAt(LocalDateTime.now());
            courseResourceService.save(resource);
            return Result.ok(resource.getResourceId());
        } catch (IOException exception) {
            return Result.fail("课程资源上传失败");
        }
    }

    @PutMapping("/{resourceId}")
    public Result<String> update(@PathVariable String resourceId,
                                 @RequestBody CourseResource request,
                                 HttpSession session) {
        CourseResource existing = courseResourceService.getById(resourceId);
        if (existing == null) return Result.fail("课程资源不存在");
        if (!auth.canModifyCourse(session, existing.getCourseCode())) return Result.fail("无权限");
        if (request.getCourseCode() != null && !existing.getCourseCode().equals(request.getCourseCode())) {
            return Result.fail("不支持跨课程移动资源");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) return Result.fail("资源标题不能为空");
        if (request.getResourceType() != null && !existing.getResourceType().equals(request.getResourceType().trim().toLowerCase())) {
            return Result.fail("资源类型由上传文件决定，不能直接修改");
        }
        String knowledgePointMessage = validateKnowledgePoint(existing.getCourseCode(), request.getKnowledgePointId());
        if (knowledgePointMessage != null) return Result.fail(knowledgePointMessage);

        existing.setTitle(request.getTitle().trim());
        existing.setChapter(request.getChapter());
        existing.setKnowledgePointId(request.getKnowledgePointId());
        courseResourceService.updateById(existing);
        return Result.ok("课程资源更新成功");
    }

    @DeleteMapping("/{resourceId}")
    public Result<Void> delete(@PathVariable String resourceId, HttpSession session) {
        CourseResource resource = courseResourceService.getById(resourceId);
        if (resource == null) return Result.fail("课程资源不存在");
        if (!auth.canModifyCourse(session, resource.getCourseCode())) return Result.fail("无权限");
        if (!courseResourceService.removeById(resourceId)) return Result.fail("课程资源删除失败");
        try {
            fileStorageService.deletePrivateFileIfExists(resource.getFileUrl());
            fileStorageService.deletePrivateFileIfExists(resource.getPreviewFileUrl());
        } catch (IOException | IllegalArgumentException ignored) {
            // Storage cleanup is best-effort; deleting a DB record must not expose a deleted resource again.
        }
        return Result.ok();
    }

    private String validateFile(MultipartFile file, String requestedType) {
        if (file == null || file.isEmpty()) return "请选择课程资源文件";
        if (file.getSize() > MAX_FILE_SIZE) return "课程资源不能超过 10MB";
        String detectedType = detectResourceType(file.getOriginalFilename());
        if (detectedType == null) return "仅支持 PPT、PDF、Word、视频、图片和文本资源";
        if (requestedType != null && !requestedType.isBlank() && !detectedType.equals(requestedType.trim().toLowerCase())) {
            return "资源类型与上传文件不匹配";
        }
        return null;
    }

    private String validateKnowledgePoint(String courseCode, String knowledgePointId) {
        if (knowledgePointId == null || knowledgePointId.isBlank()) return null;
        KnowledgePoint knowledgePoint = knowledgePointService.getById(knowledgePointId);
        if (knowledgePoint == null) return "关联知识点不存在";
        return courseCode.equals(knowledgePoint.getCourseCode()) ? null : "关联知识点不属于该课程";
    }

    private String detectResourceType(String filename) {
        if (filename == null || !filename.contains(".")) return null;
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "ppt", "pptx" -> "ppt";
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            case "mp4", "webm", "mov" -> "video";
            case "png", "jpg", "jpeg", "gif", "webp" -> "image";
            case "txt", "md" -> "text";
            default -> null;
        };
    }
}
