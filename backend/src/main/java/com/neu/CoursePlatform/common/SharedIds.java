package com.neu.CoursePlatform.common;

import java.util.UUID;

/**
 * 共享 ID 工具 — UUID v4 生成与校验。
 * 与 specs/模块接口与协作规范.md §3 定义一致：
 * "所有 ID 使用 UUID v4 字符串，由各模块生成，全局唯一。"
 *
 * 各模块在实体构造时调用 {@link #newId()} 生成新ID。
 */
public final class SharedIds {

    private SharedIds() {} // 不可实例化

    /** 生成一个新的 UUID v4 字符串（格式如 "a1b2c3d4-..."） */
    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** 校验是否为合法 UUID 格式 */
    public static boolean isValid(String id) {
        if (id == null || id.isEmpty()) return false;
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
