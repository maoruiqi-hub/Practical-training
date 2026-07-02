package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.LessonDTO;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.LessonService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

    @Mock private LessonService lessonService;
    @Mock private FileStorageService fileStorageService;
    @Mock private Auth auth;
    @Mock private HttpSession session;
    @InjectMocks private LessonController controller;

    // ============ detail ============

    @Test
    void detailReturnsDto() {
        LessonDTO dto = new LessonDTO();
        dto.setLessonTitle("Java入门");
        when(lessonService.getDetailDto("L001")).thenReturn(dto);

        Result<LessonDTO> result = controller.detail("L001", session);

        assertEquals(200, result.getCode());
        assertEquals("Java入门", result.getData().getLessonTitle());
    }

    @Test
    void detailNotFound() {
        when(lessonService.getDetailDto("L999")).thenReturn(null);

        Result<LessonDTO> result = controller.detail("L999", session);

        assertEquals(500, result.getCode());
    }

    // ============ list ============

    @Test
    void listReturnsLessons() {
        when(lessonService.listByCourseCode("CS101")).thenReturn(List.of(new Lesson()));

        Result<List<Lesson>> result = controller.list("CS101", session);

        assertEquals(200, result.getCode());
    }

    // ============ search ============

    @Test
    void searchRequiresLogin() {
        when(auth.isLoggedIn(session)).thenReturn(false);

        Result<List<Lesson>> result = controller.search("keyword", session);

        assertEquals(500, result.getCode());
    }

    @Test
    void searchReturnsResults() {
        when(auth.isLoggedIn(session)).thenReturn(true);
        when(lessonService.searchByKeyword("Java")).thenReturn(List.of(new Lesson()));

        Result<List<Lesson>> result = controller.search("Java", session);

        assertEquals(200, result.getCode());
    }

    // ============ add ============

    @Test
    void addRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = controller.add("CS101", "Lesson1", "video", "desc", null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void addSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<String> result = controller.add("CS101", "Lesson1", "video", "desc", null, session);

        ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonService).save(captor.capture());
        Lesson saved = captor.getValue();
        assertEquals(200, result.getCode());
        assertEquals("CS101", saved.getCourseCode());
        assertEquals("Lesson1", saved.getLessonTitle());
        assertEquals("video", saved.getResourceType());
    }

    @Test
    void addWithFile() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(fileStorageService.store(any(), anyString())).thenReturn("resource.pdf");
        MockMultipartFile file = new MockMultipartFile("file", "resource.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.add("CS101", "Lesson1", "document", "desc", file, session);

        assertEquals(200, result.getCode());
        ArgumentCaptor<Lesson> captor = ArgumentCaptor.forClass(Lesson.class);
        verify(lessonService).save(captor.capture());
        assertEquals("resource.pdf", captor.getValue().getResourceUrl());
    }

    @Test
    void addFileUploadFails() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("error"));
        MockMultipartFile file = new MockMultipartFile("file", "resource.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.add("CS101", "Lesson1", "document", "desc", file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("上传失败"));
    }

    // ============ update ============

    @Test
    void updateRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<String> result = controller.update("CS101", "L001", "Title", "video", "desc", null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateLessonNotFound() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        when(lessonService.getById("L001")).thenReturn(null);

        Result<String> result = controller.update("CS101", "L001", "Title", "video", "desc", null, session);

        assertEquals(500, result.getCode());
    }

    @Test
    void updateSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Lesson existing = new Lesson();
        existing.setLessonNo("L001");
        when(lessonService.getById("L001")).thenReturn(existing);

        Result<String> result = controller.update("CS101", "L001", "NewTitle", "document", "new desc", null, session);

        assertEquals(200, result.getCode());
        assertEquals("NewTitle", existing.getLessonTitle());
        assertEquals("document", existing.getResourceType());
        assertEquals("new desc", existing.getDescription());
        verify(lessonService).updateById(existing);
    }

    @Test
    void updateWithFile() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Lesson existing = new Lesson();
        when(lessonService.getById("L001")).thenReturn(existing);
        when(fileStorageService.store(any(), anyString())).thenReturn("new_resource.pdf");
        MockMultipartFile file = new MockMultipartFile("file", "resource.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.update("CS101", "L001", "Title", "document", "desc", file, session);

        assertEquals(200, result.getCode());
        assertEquals("new_resource.pdf", existing.getResourceUrl());
    }

    @Test
    void updateFileUploadFails() throws IOException {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);
        Lesson existing = new Lesson();
        when(lessonService.getById("L001")).thenReturn(existing);
        when(fileStorageService.store(any(), anyString())).thenThrow(new IOException("error"));
        MockMultipartFile file = new MockMultipartFile("file", "resource.pdf", "application/pdf", new byte[]{1});

        Result<String> result = controller.update("CS101", "L001", "Title", "document", "desc", file, session);

        assertEquals(500, result.getCode());
        assertTrue(result.getMsg().contains("上传失败"));
    }

    // ============ delete ============

    @Test
    void deleteRequiresPermission() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(false);

        Result<Void> result = controller.delete("CS101", "L001", session);

        assertEquals(500, result.getCode());
        verify(lessonService, never()).removeById(anyString());
    }

    @Test
    void deleteSuccess() {
        when(auth.canModifyCourse(session, "CS101")).thenReturn(true);

        Result<Void> result = controller.delete("CS101", "L001", session);

        assertEquals(200, result.getCode());
        verify(lessonService).removeById("L001");
    }
}
