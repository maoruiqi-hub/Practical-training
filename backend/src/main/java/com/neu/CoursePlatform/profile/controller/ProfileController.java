package com.neu.CoursePlatform.profile.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.entity.Student;
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
    private final Auth auth;

    public ProfileController(ProfileService profileService,
                            RecommendationService recommendationService,
                            IncentiveService incentiveService,
                            Auth auth) {
        this.profileService = profileService;
        this.recommendationService = recommendationService;
        this.incentiveService = incentiveService;
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
    public Result<List<CompetencyScore>> competency(@PathVariable Integer studentNo,
                                                   @PathVariable Integer courseCode,
                                                   HttpSession session) {
        if (!canViewStudentProfile(studentNo, courseCode, session)) return Result.fail("无权限");
        return Result.ok(profileService.getCompetencyScores(studentNo, courseCode));
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

    /** 模拟答题触发画像更新 | 测试用 */
    @PostMapping("/{studentNo}/{courseCode}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Integer studentNo,
                                              @PathVariable Integer courseCode,
                                              @RequestParam(defaultValue = "true") boolean correct,
                                              @RequestParam(defaultValue = "default") String taskType,
                                              @RequestParam(required = false) String abilityPointId,
                                              HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        profileService.updateProfileFromSubmission(studentNo, courseCode, correct, taskType);
        if (abilityPointId != null) {
            profileService.updateCompetencyScores(studentNo, courseCode, abilityPointId, correct);
        }
        return Result.ok(profileService.getProfileSummary(studentNo, courseCode));
    }

    /** 增加成长值 | 其他模块调用 */
    @PostMapping("/{studentNo}/{courseCode}/growth/add")
    public Result<Void> addGrowth(@PathVariable Integer studentNo,
                                  @PathVariable Integer courseCode,
                                  @RequestBody Map<String, Object> body,
                                  HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        int amount = body.get("amount") != null ? ((Number) body.get("amount")).intValue() : 0;
        String source = (String) body.getOrDefault("source", "unknown");
        String sourceId = (String) body.getOrDefault("sourceId", "");
        profileService.addGrowth(studentNo, courseCode, amount, source, sourceId);
        return Result.ok();
    }

    /** 授予徽章 | 系统自动判定 */
    @PostMapping("/{studentNo}/{courseCode}/achievements/award")
    public Result<List<Achievement>> awardAchievements(@PathVariable Integer studentNo,
                                                        @PathVariable Integer courseCode,
                                                        @RequestBody Map<String, Object> body,
                                                        HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        int totalCorrect = body.get("totalCorrect") != null ? ((Number) body.get("totalCorrect")).intValue() : 0;
        int consecutiveCorrect = body.get("consecutiveCorrect") != null ? ((Number) body.get("consecutiveCorrect")).intValue() : 0;
        boolean timedComplete = body.get("timedComplete") != null && (Boolean) body.get("timedComplete");
        boolean fullScore = body.get("fullScore") != null && (Boolean) body.get("fullScore");
        int nightSessions = body.get("nightSessions") != null ? ((Number) body.get("nightSessions")).intValue() : 0;
        int helpfulFeedback = body.get("helpfulFeedback") != null ? ((Number) body.get("helpfulFeedback")).intValue() : 0;
        int selfCorrections = body.get("selfCorrections") != null ? ((Number) body.get("selfCorrections")).intValue() : 0;
        int pythonicStyleCount = body.get("pythonicStyleCount") != null ? ((Number) body.get("pythonicStyleCount")).intValue() : 0;
        List<Achievement> newBadges = incentiveService.checkAndAwardBadges(
                studentNo, courseCode, totalCorrect, consecutiveCorrect,
                timedComplete, fullScore, nightSessions, helpfulFeedback,
                selfCorrections, pythonicStyleCount);
        return Result.ok(newBadges);
    }

    /** 手动触发画像生成 */
    @PostMapping("/{studentNo}/{courseCode}/generate")
    public Result<Map<String, Object>> generateProfile(@PathVariable Integer studentNo,
                                                        @PathVariable Integer courseCode,
                                                        HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(profileService.generateProfile(studentNo, courseCode));
    }

    /** 手动触发能力评分更新 */
    @PostMapping("/{studentNo}/{courseCode}/competency/update")
    public Result<List<CompetencyScore>> updateCompetency(@PathVariable Integer studentNo,
                                                           @PathVariable Integer courseCode,
                                                           HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        return Result.ok(profileService.updateAllCompetencyScores(studentNo, courseCode));
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

    // ========== String-based cross-module API (§12.3 VARCHAR(36) compatibility) ==========

    /** 跨模块：接收游戏事件并更新画像（模块1/2/3 → 模块4） */
    @PostMapping("/event/receive")
    public Result<Map<String, Object>> receiveGameEvent(@RequestBody Map<String, Object> body) {
        String studentNo = (String) body.get("studentNo");
        String courseCode = (String) body.get("courseCode");
        String eventType = (String) body.get("eventType");

        if (studentNo == null || courseCode == null || eventType == null) {
            return Result.fail("studentNo, courseCode, eventType 不能为空");
        }

        Integer sn = Integer.parseInt(studentNo);
        Integer cc = Integer.parseInt(courseCode);

        switch (eventType) {
            case GameEventTypes.ANSWER_CORRECT -> {
                String taskType = (String) body.getOrDefault("taskType", "quiz");
                String abilityPointId = (String) body.get("abilityPointId");
                profileService.updateProfileFromSubmission(sn, cc, true, taskType);
                if (abilityPointId != null && !abilityPointId.isEmpty()) {
                    profileService.updateCompetencyScores(sn, cc, abilityPointId, true);
                }
            }
            case GameEventTypes.ANSWER_WRONG -> {
                String taskType = (String) body.getOrDefault("taskType", "quiz");
                String abilityPointId = (String) body.get("abilityPointId");
                profileService.updateProfileFromSubmission(sn, cc, false, taskType);
                if (abilityPointId != null && !abilityPointId.isEmpty()) {
                    profileService.updateCompetencyScores(sn, cc, abilityPointId, false);
                }
            }
            case GameEventTypes.FLOOR_CLEARED ->
                profileService.addGrowth(sn, cc, 80, GameEventTypes.FLOOR_CLEARED, (String) body.getOrDefault("floor", ""));
            case GameEventTypes.BOSS_DEFEATED ->
                profileService.addGrowth(sn, cc, 250, GameEventTypes.BOSS_DEFEATED, "");
            case GameEventTypes.SUPPLY_USED ->
                profileService.addGrowth(sn, cc, -10, GameEventTypes.SUPPLY_USED, (String) body.getOrDefault("supplyType", ""));
            default ->
                profileService.addGrowth(sn, cc, 5, eventType, "");
        }

        return Result.ok(profileService.getProfileSummary(sn, cc));
    }

    /** 跨模块：获取画像摘要（String ID，供模块5等调用） */
    @GetMapping("/string/{studentNo}/{courseCode}")
    public Result<Map<String, Object>> summaryStr(@PathVariable String studentNo,
                                                   @PathVariable String courseCode) {
        return Result.ok(profileService.getProfileSummaryStr(studentNo, courseCode));
    }

    /** 跨模块：增加成长值（String ID） */
    @PostMapping("/string/{studentNo}/{courseCode}/growth")
    public Result<Void> addGrowthStr(@PathVariable String studentNo,
                                      @PathVariable String courseCode,
                                      @RequestBody Map<String, Object> body) {
        int amount = body.get("amount") != null ? ((Number) body.get("amount")).intValue() : 0;
        String source = (String) body.getOrDefault("source", "unknown");
        String sourceId = (String) body.getOrDefault("sourceId", "");
        profileService.addGrowthStr(studentNo, courseCode, amount, source, sourceId);
        return Result.ok();
    }
}
