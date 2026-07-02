package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.CourseResource;
import com.neu.CoursePlatform.entity.KnowledgePoint;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseResourceService;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.KnowledgePointService;
import com.neu.CoursePlatform.service.OfficePreviewResult;
import com.neu.CoursePlatform.service.OfficePreviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseResourceControllerTest {

    @Mock
    private CourseResourceService courseResourceService;

    @Mock
    private CourseService courseService;

    @Mock
    private KnowledgePointService knowledgePointService;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private Auth auth;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private OfficePreviewService officePreviewService;

    @Mock
    private HttpSession session;

    @InjectMocks
    private CourseResourceController courseResourceController;

    @Test
    void authorizedTeacherCanUploadPdfResource() throws Exception {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo("2");
        MockMultipartFile file = new MockMultipartFile("file", "outline.pdf", "application/pdf", "content".getBytes());
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(courseService.getById("1")).thenReturn(new Course());
        when(auth.getTeacher(session)).thenReturn(teacher);
        when(fileStorageService.store(any(), anyString())).thenReturn("resource/CourseResource/file.pdf");
        when(officePreviewService.generatePreview(anyString(), anyString())).thenReturn(OfficePreviewResult.notRequired());

        Result<String> result = courseResourceController.upload(
                "1", "Course outline", "Chapter 1", null, "pdf", file, session);

        ArgumentCaptor<CourseResource> captor = ArgumentCaptor.forClass(CourseResource.class);
        verify(courseResourceService).save(captor.capture());
        assertEquals(200, result.getCode());
        assertEquals("pdf", captor.getValue().getResourceType());
        assertEquals("2", captor.getValue().getUploadedBy());
        assertEquals("resource/CourseResource/file.pdf", captor.getValue().getFileUrl());
    }

    @Test
    void unsupportedFileTypeIsRejectedBeforeStorage() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "program.exe", "application/octet-stream", new byte[]{1});
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(courseService.getById("1")).thenReturn(new Course());

        Result<String> result = courseResourceController.upload("1", null, null, null, null, file, session);

        assertEquals(500, result.getCode());
        verify(fileStorageService, never()).store(any(), anyString());
        verify(courseResourceService, never()).save(any(CourseResource.class));
    }

    @Test
    void resourceCannotReferenceKnowledgePointFromAnotherCourse() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slides.pptx", "application/octet-stream", new byte[]{1});
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setCourseCode("2");
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(courseService.getById("1")).thenReturn(new Course());
        when(knowledgePointService.getById("10")).thenReturn(knowledgePoint);

        Result<String> result = courseResourceController.upload("1", null, null, "10", "ppt", file, session);

        assertEquals(500, result.getCode());
        verify(fileStorageService, never()).store(any(), anyString());
    }

    @Test
    void unauthorizedResourceDeletionIsRejected() {
        CourseResource resource = new CourseResource();
        resource.setResourceId("1");
        resource.setCourseCode("1");
        when(courseResourceService.getById("1")).thenReturn(resource);
        when(auth.canModifyCourse(session, "1")).thenReturn(false);

        Result<Void> result = courseResourceController.delete("1", session);

        assertEquals(500, result.getCode());
        verify(courseResourceService, never()).removeById(anyString());
    }

    @Test
    void authenticatedUserCanOpenProtectedResourceContent() throws Exception {
        CourseResource resource = new CourseResource();
        resource.setResourceId("1");
        resource.setFileUrl("private-resource/CourseResource/file.pdf");
        resource.setOriginalFilename("outline.pdf");
        when(courseResourceService.getById("1")).thenReturn(resource);
        when(fileStorageService.readPrivateFile(resource.getFileUrl())).thenReturn("content".getBytes());
        when(fileStorageService.getPrivateFileContentType(resource.getFileUrl())).thenReturn("application/pdf");

        ResponseEntity<byte[]> response = courseResourceController.content("1", false, false, session);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertEquals("content", new String(response.getBody()));
    }

    @Test
    void officeResourceIsSavedWithUnavailablePreviewStatusWhenConverterIsDisabled() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "slides.pptx", "application/octet-stream", new byte[]{1});
        when(auth.canModifyCourse(session, "1")).thenReturn(true);
        when(courseService.getById("1")).thenReturn(new Course());
        when(fileStorageService.store(any(), anyString())).thenReturn("private-resource/CourseResource/slides.pptx");
        when(officePreviewService.generatePreview("private-resource/CourseResource/slides.pptx", "ppt"))
                .thenReturn(OfficePreviewResult.unavailable());

        Result<String> result = courseResourceController.upload("1", null, null, null, "ppt", file, session);

        ArgumentCaptor<CourseResource> captor = ArgumentCaptor.forClass(CourseResource.class);
        verify(courseResourceService).save(captor.capture());
        assertEquals(200, result.getCode());
        assertEquals("unavailable", captor.getValue().getPreviewStatus());
        assertEquals(null, captor.getValue().getPreviewFileUrl());
    }
}
