package com.neu.CoursePlatform.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void okShouldReturn200WithData() {
        Result<String> result = Result.ok("hello");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMsg());
        assertEquals("hello", result.getData());
    }

    @Test
    void okWithoutDataShouldReturn200WithNullData() {
        Result<Void> result = Result.ok();
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void failShouldReturn500WithMessage() {
        Result<Void> result = Result.fail("参数错误");
        assertEquals(500, result.getCode());
        assertEquals("参数错误", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void serviceUnavailableShouldReturn503WithMessage() {
        Result<String> result = Result.serviceUnavailable("AI 服务不可用");
        assertEquals(503, result.getCode());
        assertEquals("AI 服务不可用", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void resultShouldSupportSetterMethods() {
        Result<Integer> result = new Result<>();
        result.setCode(200);
        result.setMsg("自定义消息");
        result.setData(42);
        assertEquals(200, result.getCode());
        assertEquals("自定义消息", result.getMsg());
        assertEquals(42, result.getData());
    }

    @Test
    void okWithNullDataShouldWork() {
        Result<String> result = Result.ok(null);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMsg());
        assertNull(result.getData());
    }
}
