package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.LessonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseService courseService;
    private final LessonService lessonService;
    private final Auth auth;

    public CourseController(CourseService courseService, LessonService lessonService, Auth auth) {
        this.courseService = courseService;
        this.lessonService = lessonService;
        this.auth = auth;
    }

    private CourseDTO toDto(Course c) {
        CourseDTO dto = new CourseDTO();
        dto.setCourseCode(c.getCourseCode());
        dto.setCourseName(c.getCourseName());
        dto.setTeacher(c.getTeacher());
        dto.setCredits(c.getCredits());
        dto.setHours(c.getHours());
        dto.setCoverUrl(c.getCoverUrl());
        dto.setLessonCount(lessonService.listByCourseCode(c.getCourseCode()).size());
        return dto;
    }

    /** 模糊搜索课程 | 登录用户 */
    @GetMapping("/search")
    public Result<List<CourseDTO>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        List<CourseDTO> dtos = new ArrayList<>();
        for (Course c : courseService.searchByKeyword(keyword)) {
            dtos.add(toDto(c));
        }
        return Result.ok(dtos);
    }

    /** 查看课程课时列表 | 登录用户 */
    @GetMapping("/{courseCode}/lessons")
    public Result<List<Lesson>> lessons(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(courseService.listLessons(courseCode));
    }

    /** 全部课程列表 | admin */
    @GetMapping("/list")
    public Result<List<CourseDTO>> list(HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        List<CourseDTO> dtos = new ArrayList<>();
        for (Course c : courseService.list()) {
            dtos.add(toDto(c));
        }
        return Result.ok(dtos);
    }

    /** 按编号查课程 | admin */
    @GetMapping("/{courseCode}")
    public Result<Course> getByCode(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        Course c = courseService.getById(courseCode);
        return c != null ? Result.ok(c) : Result.fail("课程不存在");
    }

    /** 新增课程（支持上传封面） | admin */
    @PostMapping
    public Result<String> add(@RequestParam String courseName,
                              @RequestParam String teacher,
                              @RequestParam Integer credits,
                              @RequestParam Integer hours,
                              @RequestParam(required = false) MultipartFile file,
                              HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        Course course = new Course();
        course.setCourseName(courseName);
        course.setTeacher(teacher);
        course.setCredits(credits);
        course.setHours(hours);
        if (file != null && !file.isEmpty()) {
            try {
                String dir = "../resource/CourseResource/";
                File folder = new File(dir);
                if (!folder.exists()) folder.mkdirs();
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                java.nio.file.Files.write(new File(dir + filename).toPath(), file.getBytes());
                course.setCoverUrl((dir + filename).replace("../", ""));
            } catch (IOException e) {
                return Result.fail("封面上传失败");
            }
        }
        courseService.save(course);
        return Result.ok("课程创建成功");
    }

    /** 删除课程 | admin */
    @DeleteMapping("/{courseCode}")
    public Result<Void> delete(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isAdmin(session)) return Result.fail("无权限");
        courseService.removeById(courseCode);
        return Result.ok();
    }

    /** 修改课程（支持上传封面） | admin/授课教师 */
    @PutMapping("/{courseCode}")
    public Result<String> update(@PathVariable String courseCode,
                                 @RequestParam String courseName,
                                 @RequestParam String teacher,
                                 @RequestParam Integer credits,
                                 @RequestParam Integer hours,
                                 @RequestParam(required = false) MultipartFile file,
                                 HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        Course course = courseService.getById(courseCode);
        if (course == null) return Result.fail("课程不存在");
        course.setCourseName(courseName);
        course.setTeacher(teacher);
        course.setCredits(credits);
        course.setHours(hours);
        if (file != null && !file.isEmpty()) {
            try {
                String dir = "../resource/CourseResource/";
                File folder = new File(dir);
                if (!folder.exists()) folder.mkdirs();
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                java.nio.file.Files.write(new File(dir + filename).toPath(), file.getBytes());
                course.setCoverUrl((dir + filename).replace("../", ""));
            } catch (IOException e) {
                return Result.fail("封面上传失败");
            }
        }
        courseService.updateById(course);
        return Result.ok("课程更新成功");
    }
}
