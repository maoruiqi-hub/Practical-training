package com.neu.CoursePlatform.common;

/**
 * 爬塔游戏事件类型 — 字符串常量。
 * 与 specs/模块接口与协作规范.md §14.3 定义完全一致。
 * 所有模块发送/接收游戏事件时统一使用此处常量，避免拼写错误。
 *
 * 使用方式：发事件时用 {@code GameEventTypes.ANSWER_CORRECT}，
 * 接收端 switch/if 比较同一常量，编译期即可发现不一致。
 */
public final class GameEventTypes {

    private GameEventTypes() {} // 不可实例化

    // ============ §14.3.1 答题类事件（模块3 发出） ============

    /** 学生答对一道题 */
    public static final String ANSWER_CORRECT = "answer_correct";
    /** 学生答错一道题 */
    public static final String ANSWER_WRONG = "answer_wrong";
    /** 学生跳过一道题 */
    public static final String ANSWER_SKIPPED = "answer_skipped";

    // ============ §14.3.2 楼层类事件（模块1/模块3 发出） ============

    /** 某层所有题完成（正确率≥通关线） */
    public static final String FLOOR_CLEARED = "floor_cleared";
    /** Boss层通关 */
    public static final String BOSS_DEFEATED = "boss_defeated";
    /** 精英怪层通关 */
    public static final String ELITE_DEFEATED = "elite_defeated";
    /** 某层尝试后未通过 */
    public static final String FLOOR_FAILED = "floor_failed";

    // ============ §14.3.3 补给类事件（模块2/前端 发出） ============

    /** 学生使用了补给 */
    public static final String SUPPLY_USED = "supply_used";
    /** 学生使用了提示 */
    public static final String HINT_USED = "hint_used";
    /** 学生召唤AI导师 */
    public static final String AI_TUTOR_CALLED = "ai_tutor_called";

    // ============ §14.3.4 系统类事件（模块4 内部/定时触发） ============

    /** HP < 30，通知模块5记录风险事件 */
    public static final String HP_CRITICAL = "hp_critical";
    /** 同一知识点连续失败≥3次或卡顿超10分钟 */
    public static final String STUCK_DETECTED = "stuck_detected";
    /** 每日0点精力重置 */
    public static final String DAILY_RESET = "daily_reset";
    /** 连胜达到3 */
    public static final String STREAK_3 = "streak_3";
    /** 连胜达到5 */
    public static final String STREAK_5 = "streak_5";
    /** 连胜达到8 */
    public static final String STREAK_8 = "streak_8";
    /** 连胜达到10 */
    public static final String STREAK_10 = "streak_10";
    /** EXP达到升级阈值 */
    public static final String LEVEL_UP = "level_up";

    // ============ 模块5 额外关注的风险事件 ============

    /** 连续3天未登录（模块4检测 → 模块5记录） */
    public static final String INACTIVE = "inactive";
}
