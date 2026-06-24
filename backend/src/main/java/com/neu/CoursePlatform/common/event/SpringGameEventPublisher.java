package com.neu.CoursePlatform.common.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringGameEventPublisher implements GameEventPublisher {
    private final ApplicationEventPublisher publisher;

    public SpringGameEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(GameEvent event) {
        publisher.publishEvent(event);
    }
}
