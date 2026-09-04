package com.neu.CoursePlatform.profile.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Student;
import com.neu.CoursePlatform.service.StudentAbilityProjectionService;
import com.neu.CoursePlatform.profile.entity.*;
import com.neu.CoursePlatform.profile.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final RecommendationService recommendationService;
    private final IncentiveService incentiveService;
    private final StudentAbilityProjectionService abilityProjectionService;
    private final Auth auth;

    public ProfileController(ProfileService profileService,
                            RecommendationService recommendationService,
                            IncentiveService incentiveService,
                            StudentAbilityProjectionService abilityProjectionService,
                            Auth auth) {
        this.profileService = profileService;
        this.recommendationService = recommendationService;
        this.incentiveService = incentiveService;
        this.abilityProjectionService = abilityProjectionService;
        this.auth = auth;
    }

    /** 判断当前用户是否有权查看指定学生的画像 */
    private boolean canViewStudentProfile(Integer studentNo, Integer courseCode, HttpSession session) {
        // 学生本人
        Student login = (Student) session.getAttribute("student");
        if (login != null && login.getStudentNo().equals(studentNo.toString())) return true;
        // 管理员
        if (auth.isAdmin(session)) return true;
        // 教师（教授该课程）
        return auth.canModifyCourse(session, courseCode.toString());
    }

    /** 教师端：获取课程下所有学生的画像摘要列表 */
    @GetMapping("/teacher/course/{courseCode}/students")
    public Result<List<Map<String, Object>>> courseStudents(@PathVariable Integer courseCode,
                                                             HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        if (!auth.isAdmin(session) && !auth.canModifyCourse(session, courseCode.toString())) {
            return Result.fail("无权限查看该课程的学生画像");
        }
        return Result.ok(profileService.listCourseStudentProfiles(courseCode));
    }

    /** 爬塔地图：学生视角的知识点楼层状态（§14.6） */
    @GetMapping("/{studentNo}/{courseCode}/tower-map")
    public Result<List<Map<String, Object>>> towerMap(@PathVariable Integer studentNo,
                                                       @PathVariable Integer courseCode,
                                                       HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(profileService.getTowerMap(studentNo, courseCode));
    }

    /** 获取画像总览 | 学生本人 */
    @GetMapping("/{studentNo}/{courseCode}")
    public Result<Map<String, Object>> summary(@PathVariable Integer studentNo,
                                              @PathVariable Integer courseCode,
                                              HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(profileService.getProfileSummary(studentNo, courseCode));
    }

    /** 获取能力评分 | 学生本人 */
    @GetMapping("/{studentNo}/{courseCode}/competency")
    public Result<List<Map<String, Object>>> competency(@PathVariable Integer studentNo,
                                                        @PathVariable Integer courseCode,
                                                        HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(abilityProjectionService.coursePoints(studentNo.toString(), courseCode.toString()));
    }

    /** 获取由假能力点映射得到的真能力评分 | 学生本人或任课教师 */
    @GetMapping("/{studentNo}/{courseCode}/true-competency")
    public Result<List<Map<String, Object>>> trueCompetency(@PathVariable Integer studentNo,
                                                             @PathVariable Integer courseCode,
                                                             HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(abilityProjectionService.trueCompetencies(
                studentNo.toString(), courseCode.toString()));
    }

    /** 获取推荐列表 | 学生本人 */
    @GetMapping("/{studentNo}/{courseCode}/recommendations")
    public Result<List<Recommendation>> recommendations(@PathVariable Integer studentNo,
                                                       @PathVariable Integer courseCode,
                                                       HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        List<Recommendation> recs = recommendationService.getRecommendations(studentNo, courseCode);
        if (recs.isEmpty()) recs = recommendationService.generateRecommendations(studentNo, courseCode);
        return Result.ok(recs);
    }

    /** 刷新推荐 | 学生本人 */
    @PostMapping("/{studentNo}/{courseCode}/recommendations/generate")
    public Result<List<Recommendation>> generateRecommendations(@PathVariable Integer studentNo,
                                                                @PathVariable Integer courseCode,
                                                                HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(recommendationService.generateRecommendations(studentNo, courseCode));
    }

    /** 推荐反馈 | 学生本人 */
    @PutMapping("/recommendations/{id}/feedback")
    public Result<Void> feedback(@PathVariable String id, @RequestParam String feedback,
                                HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        recommendationService.recordFeedback(id, feedback);
        return Result.ok();
    }

    /** 获取成就列表 | 学生本人 */
    @GetMapping("/{studentNo}/{courseCode}/achievements")
    public Result<List<Achievement>> achievements(@PathVariable Integer studentNo,
                                                 @PathVariable Integer courseCode,
                                                 HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(incentiveService.getAchievements(studentNo, courseCode));
    }

    /** 获取称号 | 学生本人 */
    @GetMapping("/{studentNo}/{courseCode}/title")
    public Result<String> title(@PathVariable Integer studentNo,
                               @PathVariable Integer courseCode,
                               HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(incentiveService.getTitle(studentNo, courseCode));
    }

    /** 排行榜 | 登录用户 */
    @GetMapping("/leaderboard")
    public Result<List<Map<String, Object>>> leaderboard(@RequestParam(defaultValue = "1") Integer courseCode,
                                                        @RequestParam(defaultValue = "exp") String type,
                                                        HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(incentiveService.getLeaderboard(courseCode, type));
    }

    /** 能力评分变更历史 | 学生本人 (R4.6) */
    @GetMapping("/{studentNo}/{courseCode}/competency/history")
    public Result<List<Map<String, Object>>> competencyHistory(@PathVariable Integer studentNo,
                                                                @PathVariable Integer courseCode,
                                                                @RequestParam(required = false) String abilityPointId,
                                                                HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(profileService.getCompetencyHistory(studentNo, courseCode, abilityPointId));
    }

    /** 成长值变更明细 | 学生本人 (R6.6) */
    @GetMapping("/{studentNo}/{courseCode}/growth/history")
    public Result<List<Map<String, Object>>> growthHistory(@PathVariable Integer studentNo,
                                                            @PathVariable Integer courseCode,
                                                            HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(profileService.getGrowthHistory(studentNo, courseCode));
    }

    /** 测验后反馈摘要 | 学生本人 (R7.1) */
    @PostMapping("/{studentNo}/{courseCode}/feedback")
    public Result<Map<String, Object>> testFeedback(@PathVariable Integer studentNo,
                                                     @PathVariable Integer courseCode,
                                                     HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(profileService.generateTestFeedback(studentNo, courseCode));
    }

}
