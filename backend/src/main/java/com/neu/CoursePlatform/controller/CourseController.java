package com.neu.CoursePlatform.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.dto.CourseDTO;
import com.neu.CoursePlatform.entity.Course;
import com.neu.CoursePlatform.entity.Lesson;
import com.neu.CoursePlatform.entity.Teacher;
import com.neu.CoursePlatform.service.CourseService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import com.neu.CoursePlatform.service.FileStorageService;
import com.neu.CoursePlatform.service.TeacherService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final FileStorageService fileStorageService;
    private final TeacherService teacherService;
    private final CourseGameConfigService courseGameConfigService;
    private final Auth auth;

    @Value("${game.mode.enabled:false}")
    private boolean gameModeEnabled;

    public CourseController(CourseService courseService,
                            FileStorageService fileStorageService,
                            TeacherService teacherService,
                            CourseGameConfigService courseGameConfigService,
                            Auth auth) {
        this.courseService = courseService;
        this.fileStorageService = fileStorageService;
        this.teacherService = teacherService;
        this.courseGameConfigService = courseGameConfigService;
        this.auth = auth;
    }

    /** 爬塔模式开关：读取时默认关闭，避免未配置课程误发游戏事件。 */
    @GetMapping("/{courseCode}/config")
    public Result<Map<String, Object>> gameConfig(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (courseService.getById(courseCode) == null) return Result.fail("课程不存在");
        return Result.ok(Map.of("courseId", courseCode,
                "game_mode_enabled", courseGameConfigService.isEnabled(courseCode)));
    }

    @PutMapping("/{courseCode}/config")
    public Result<Void> updateGameConfig(@PathVariable String courseCode,
                                         @RequestBody Map<String, Boolean> body,
                                         HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        Boolean enabled = body == null ? null : body.get("game_mode_enabled");
        if (enabled == null) return Result.fail("缺少 game_mode_enabled");
        return courseGameConfigService.updateEnabled(courseCode, enabled) ? Result.ok() : Result.fail("课程游戏配置保存失败");
    }

    /** 模糊搜索课程 | 登录用户 */
    @GetMapping("/search")
    public Result<List<CourseDTO>> search(@RequestParam String keyword, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(courseService.searchDtoByKeyword(keyword));
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
        return Result.ok(courseService.listDto());
    }

    /** 按编号查课程 | admin */
    @GetMapping("/{courseCode}")
    public Result<Course> getByCode(@PathVariable String courseCode, HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Course c = courseService.getById(courseCode);
        return c != null ? Result.ok(c) : Result.fail("课程不存在");
    }

    /** 新增课程（支持上传封面） | admin */
    @PostMapping
    public Result<String> add(@RequestParam String courseName,
                               @RequestParam(required = false) String teacher,
                               @RequestParam(required = false) String teacherNo,
                               @RequestParam Integer credits,
                               @RequestParam Integer hours,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String applicableMajor,
                               @RequestParam(required = false) String courseObjectives,
                               @RequestParam(required = false) MultipartFile file,
                               HttpSession session) {
        Teacher currentTeacher = auth.getTeacher(session);
        if (!auth.isAdmin(session) && currentTeacher == null) return Result.fail("无权限");
        if (courseName.isBlank() || credits < 0 || hours < 0) return Result.fail("课程名称、学分和学时不合法");
        Teacher assigned = resolveAssignedTeacher(auth.isAdmin(session), currentTeacher, teacher, teacherNo);
        if (assigned == null && teacherNo != null && !teacherNo.isBlank()) return Result.fail("授课教师不存在");
        String assignedTeacher = assigned == null ? teacher : assigned.getName();
        if (!auth.isAdmin(session)) assignedTeacher = currentTeacher.getName();
        if (assignedTeacher == null || assignedTeacher.isBlank()) return Result.fail("请填写授课教师");
        Course course = new Course();
        course.setCourseName(courseName);
        course.setTeacher(assignedTeacher);
        course.setTeacherNo(assigned == null ? null : assigned.getTeacherNo());
        course.setCredits(credits);
        course.setHours(hours);
        course.setDescription(description);
        course.setApplicableMajor(applicableMajor);
        course.setCourseObjectives(courseObjectives);
        if (file != null && !file.isEmpty()) {
            try {
                course.setCoverUrl(fileStorageService.store(file, "../resource/CourseResource/"));
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
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        courseService.removeById(courseCode);
        return Result.ok();
    }

    /** 修改课程（支持上传封面） | admin/授课教师 */
    @PutMapping("/{courseCode}")
    public Result<String> update(@PathVariable String courseCode,
                                  @RequestParam String courseName,
                                  @RequestParam(required = false) String teacher,
                                  @RequestParam(required = false) String teacherNo,
                                  @RequestParam Integer credits,
                                  @RequestParam Integer hours,
                                  @RequestParam(required = false) String description,
                                  @RequestParam(required = false) String applicableMajor,
                                  @RequestParam(required = false) String courseObjectives,
                                  @RequestParam(required = false) MultipartFile file,
                                 HttpSession session) {
        if (!auth.canModifyCourse(session, courseCode)) return Result.fail("无权限");
        Course course = courseService.getById(courseCode);
        if (course == null) return Result.fail("课程不存在");
        if (courseName.isBlank() || credits < 0 || hours < 0) return Result.fail("课程名称、学分和学时不合法");
        Teacher assigned = resolveAssignedTeacher(auth.isAdmin(session), currentTeacher(session), teacher, teacherNo);
        if (assigned == null && teacherNo != null && !teacherNo.isBlank()) return Result.fail("授课教师不存在");
        String assignedTeacher = auth.isAdmin(session) ? (assigned == null ? teacher : assigned.getName()) : course.getTeacher();
        if (assignedTeacher == null || assignedTeacher.isBlank()) return Result.fail("请填写授课教师");
        course.setCourseName(courseName);
        course.setTeacher(assignedTeacher);
        if (auth.isAdmin(session) && assigned != null) course.setTeacherNo(assigned.getTeacherNo());
        course.setCredits(credits);
        course.setHours(hours);
        course.setDescription(description);
        course.setApplicableMajor(applicableMajor);
        course.setCourseObjectives(courseObjectives);
        if (file != null && !file.isEmpty()) {
            try {
                course.setCoverUrl(fileStorageService.store(file, "../resource/CourseResource/"));
            } catch (IOException e) {
                return Result.fail("封面上传失败");
            }
        }
        courseService.updateById(course);
        return Result.ok("课程更新成功");
    }

    private Teacher currentTeacher(HttpSession session) {
        return auth.getTeacher(session);
    }

    private Teacher resolveAssignedTeacher(boolean administrator, Teacher currentTeacher,
                                           String teacherName, String teacherNo) {
        if (!administrator) return currentTeacher;
        if (teacherNo == null || teacherNo.isBlank()) return null;
        return teacherService.getById(teacherNo);
    }
}
