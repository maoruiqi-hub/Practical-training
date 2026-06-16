package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.LessonDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.LessonService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lesson")
public class LessonController {

    private final LessonService lessonService;
    private final CourseService courseService;
    private final Auth auth;

    public LessonController(LessonService lessonService, CourseService courseService, Auth auth) {
        this.lessonService = lessonService;
        this.courseService = courseService;
        this.auth = auth;
    }

    /** 课时详情（含课程名、教师名） | 登录用户 */
    @GetMapping("/detail/{lessonNo}")
    public Result<LessonDTO> detail(@PathVariable String lessonNo, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Lesson l = lessonService.getById(lessonNo);
        if (l == null) return Result.fail("课时不存在");
        LessonDTO dto = new LessonDTO();
        dto.setLessonNo(l.getLessonNo());
        dto.setCourseCode(l.getCourseCode());
        dto.setLessonTitle(l.getLessonTitle());
        dto.setResourceType(l.getResourceType());
        dto.setResourceUrl(l.getResourceUrl());
        dto.setDescription(l.getDescription());
        Course c = courseService.getById(l.getCourseCode());
        if (c != null) {
            dto.setCourseName(c.getCourseName());
            dto.setTeacherName(c.getTeacher());
        }
        return Result.ok(dto);
    }

    /** 查看某课程的课时列表 | 登录用户 */
    @GetMapping("/{courseCode}")
    public Result<List<Lesson>> list(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(lessonService.listByCourseCode(courseCode));
    }

    /** 模糊搜索课时 | 登录用户 */
    @GetMapping("/search")
    public Result<List<Lesson>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(lessonService.searchByKeyword(keyword));
    }

    /** 新增课时（支持上传资源文件） | admin/授课教师 */
    @PostMapping
    public Result<String> add(@RequestParam String courseCode,
                              @RequestParam String lessonTitle,
                              @RequestParam String resourceType,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) MultipartFile file,
                              HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");

        Lesson lesson = new Lesson();
        lesson.setCourseCode(courseCode);
        lesson.setLessonTitle(lessonTitle);
        lesson.setResourceType(resourceType);
        lesson.setDescription(description);

        if (file != null && !file.isEmpty()) {
            try {
                String dir = "resource/LessonResource/";
                File folder = new File(dir);
                if (!folder.exists()) folder.mkdirs();
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                file.transferTo(new File(dir + filename));
                lesson.setResourceUrl(dir + filename);
            } catch (IOException e) {
                return Result.fail("文件上传失败");
            }
        }

        lessonService.save(lesson);
        return Result.ok("课时创建成功");
    }

    /** 修改课时 | admin/授课教师 */
    @PutMapping("/{courseCode}/{lessonNo}")
    public Result<Void> update(@PathVariable String courseCode, @PathVariable String lessonNo,
                               @RequestBody Lesson lesson, HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        lesson.setLessonNo(lessonNo);
        lessonService.updateById(lesson);
        return Result.ok();
    }

    /** 删除课时 | admin/授课教师 */
    @DeleteMapping("/{courseCode}/{lessonNo}")
    public Result<Void> delete(@PathVariable String courseCode, @PathVariable String lessonNo,
                               HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        lessonService.removeById(lessonNo);
        return Result.ok();
    }
}
