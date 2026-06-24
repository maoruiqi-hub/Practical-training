package com.neu.CoursePlatform.common.game;

/**
 * An in-process contract. Module 4 can replace the default publisher with its
 * state-machine implementation without Module 1 or Module 3 calling HTTP.
 */
public interface GameEventPublisher {
    void publish(TowerGameEvent event);
}
