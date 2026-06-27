package com.neu.CoursePlatform.profile.controller;

import com.neu.CoursePlatform.common.Auth;
import com.neu.CoursePlatform.common.Result;
import com.neu.CoursePlatform.common.SharedIds;
import com.neu.CoursePlatform.common.event.GameEvent;
import com.neu.CoursePlatform.profile.entity.Achievement;
import com.neu.CoursePlatform.profile.entity.CompetencyScore;
import com.neu.CoursePlatform.profile.entity.Recommendation;
import com.neu.CoursePlatform.profile.service.IncentiveService;
import com.neu.CoursePlatform.profile.service.ProfileService;
import com.neu.CoursePlatform.profile.service.RecommendationService;
import com.neu.CoursePlatform.service.CourseGameConfigService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模块4规范路径适配层。
 *
 * 本类补齐
 * specs/模块接口与协作规范.md 中约定的 /api/students/** 入口。
 */
@RestController
@RequestMapping("/api")
public class ProfileApiController {

    private final ProfileService profileService;
    private final RecommendationService recommendationService;
    private final IncentiveService incentiveService;
    private final CourseGameConfigService gameConfigService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Auth auth;

    public ProfileApiController(ProfileService profileService,
                                RecommendationService recommendationService,
                                IncentiveService incentiveService,
                                CourseGameConfigService gameConfigService,
                                ApplicationEventPublisher applicationEventPublisher,
                                Auth auth) {
        this.profileService = profileService;
        this.recommendationService = recommendationService;
        this.incentiveService = incentiveService;
        this.gameConfigService = gameConfigService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.auth = auth;
    }

    @GetMapping("/students/{studentId}/profile")
    public Result<Map<String, Object>> profile(@PathVariable String studentId,
                                               @RequestParam(name = "course_id", required = false) String courseId,
                                               @RequestParam(name = "courseCode", required = false) String courseCode,
                                               HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(profileService.getProfileSummary(studentNo, courseNo));
    }

    @PostMapping("/students/{studentId}/profile/generate")
    public Result<Map<String, Object>> generateProfile(@PathVariable String studentId,
                                                       @RequestParam(name = "course_id", required = false) String courseId,
                                                       @RequestParam(name = "courseCode", required = false) String courseCode,
                                                       HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(profileService.generateProfile(studentNo, courseNo));
    }

    @GetMapping("/students/{studentId}/competency")
    public Result<List<CompetencyScore>> competency(@PathVariable String studentId,
                                                    @RequestParam(name = "course_id", required = false) String courseId,
                                                    @RequestParam(name = "courseCode", required = false) String courseCode,
                                                    HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(profileService.getCompetencyScores(studentNo, courseNo));
    }

    @PostMapping("/students/{studentId}/competency/update")
    public Result<List<CompetencyScore>> updateCompetency(@PathVariable String studentId,
                                                          @RequestParam(name = "course_id", required = false) String courseId,
                                                          @RequestParam(name = "courseCode", required = false) String courseCode,
                                                          HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(profileService.updateAllCompetencyScores(studentNo, courseNo));
    }

    @GetMapping("/students/{studentId}/recommendations")
    public Result<List<Recommendation>> recommendations(@PathVariable String studentId,
                                                        @RequestParam(name = "course_id", required = false) String courseId,
                                                        @RequestParam(name = "courseCode", required = false) String courseCode,
                                                        HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        List<Recommendation> recs = recommendationService.getRecommendations(studentNo, courseNo);
        if (recs.isEmpty()) recs = recommendationService.generateRecommendations(studentNo, courseNo);
        return Result.ok(recs);
    }

    @PostMapping("/students/{studentId}/recommendations/generate")
    public Result<List<Recommendation>> generateRecommendations(@PathVariable String studentId,
                                                                @RequestParam(name = "course_id", required = false) String courseId,
                                                                @RequestParam(name = "courseCode", required = false) String courseCode,
                                                                HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(recommendationService.generateRecommendations(studentNo, courseNo));
    }

    @GetMapping("/students/{studentId}/tower-map")
    public Result<List<Map<String, Object>>> towerMap(@PathVariable String studentId,
                                                      @RequestParam(name = "course_id", required = false) String courseId,
                                                      @RequestParam(name = "courseCode", required = false) String courseCode,
                                                      HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(profileService.getTowerMap(studentNo, courseNo));
    }

    @PostMapping("/students/{studentId}/growth/add")
    public Result<Void> addGrowth(@PathVariable String studentId,
                                  @RequestParam(name = "course_id", required = false) String courseId,
                                  @RequestParam(name = "courseCode", required = false) String courseCode,
                                  @RequestBody Map<String, Object> body,
                                  HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        int amount = body.get("amount") instanceof Number number ? number.intValue() : 0;
        String source = String.valueOf(body.getOrDefault("source", "unknown"));
        String sourceId = String.valueOf(body.getOrDefault("source_id", body.getOrDefault("sourceId", "")));
        profileService.addGrowth(studentNo, courseNo, amount, source, sourceId);
        return Result.ok();
    }

    @PostMapping("/students/{studentId}/achievements")
    public Result<List<Achievement>> awardAchievements(@PathVariable String studentId,
                                                       @RequestParam(name = "course_id", required = false) String courseId,
                                                       @RequestParam(name = "courseCode", required = false) String courseCode,
                                                       @RequestBody Map<String, Object> body,
                                                       HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        return Result.ok(incentiveService.checkAndAwardBadges(studentNo, courseNo,
                intValue(body, "totalCorrect"), intValue(body, "consecutiveCorrect"),
                boolValue(body, "timedComplete"), boolValue(body, "fullScore"),
                intValue(body, "nightSessions"), intValue(body, "helpfulFeedback"),
                intValue(body, "selfCorrections"), intValue(body, "pythonicStyleCount")));
    }

    @GetMapping("/leaderboard")
    public Result<List<Map<String, Object>>> leaderboard(@RequestParam(name = "course_id", required = false) String courseId,
                                                         @RequestParam(name = "courseCode", required = false) String courseCode,
                                                         @RequestParam(defaultValue = "progress") String type,
                                                         HttpSession session) {
        if (!auth.isLoggedIn(session)) return Result.fail("请先登录");
        Integer courseNo = parseInt(resolveCourseId(courseId, courseCode), "course_id");
        if (courseNo == null) return Result.fail("course_id 必须为数字");
        return Result.ok(incentiveService.getLeaderboard(courseNo, type));
    }

    @PostMapping("/students/{studentId}/game-event")
    public Result<Map<String, Object>> receiveGameEvent(@PathVariable String studentId,
                                                        @RequestBody Map<String, Object> body) {
        String courseId = stringValue(body, "course_id", "courseId", "courseCode");
        String eventType = stringValue(body, "event_type", "eventType");
        if (courseId == null || eventType == null) return Result.fail("course_id 和 event_type 不能为空");
        Integer studentNo = parseInt(studentId, "studentId");
        Integer courseNo = parseInt(courseId, "course_id");
        if (studentNo == null || courseNo == null) return Result.fail("studentId 和 course_id 必须为数字");
        if (!gameConfigService.isEnabled(courseId)) {
            return Result.ok(profileService.getProfileSummary(studentNo, courseNo));
        }
        applicationEventPublisher.publishEvent(GameEvent.builder()
                .eventId(SharedIds.newId())
                .eventType(eventType)
                .studentId(studentId)
                .courseId(courseId)
                .sourceId(stringValue(body, "source_id", "sourceId"))
                .occurredAt(LocalDateTime.now())
                .payload(body)
                .build());
        return Result.ok(profileService.getProfileSummary(studentNo, courseNo));
    }

    private static String resolveCourseId(String courseId, String courseCode) {
        return courseId != null ? courseId : courseCode;
    }

    private static Integer parseInt(String value, String field) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int intValue(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static boolean boolValue(Map<String, Object> body, String key) {
        Object value = body.get(key);
        return value instanceof Boolean bool && bool;
    }

    private static String stringValue(Map<String, Object> body, String... keys) {
        for (String key : keys) {
            Object value = body.get(key);
            if (value != null && !String.valueOf(value).isBlank()) return String.valueOf(value);
        }
        return null;
    }
}
