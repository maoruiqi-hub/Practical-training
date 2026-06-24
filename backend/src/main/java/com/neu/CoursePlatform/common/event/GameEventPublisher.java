package com.neu.CoursePlatform.common.event;

/** 供模块 1、3 发布游戏事件；模块 4 可订阅同一事件契约。 */
public interface GameEventPublisher {
    void publish(GameEvent event);
}
