package com.neu.CoursePlatform.common.game;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringGameEventPublisher implements GameEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringGameEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(TowerGameEvent event) {
        applicationEventPublisher.publishEvent(event);
    }
}
