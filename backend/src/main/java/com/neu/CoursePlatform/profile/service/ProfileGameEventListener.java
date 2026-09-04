package com.neu.CoursePlatform.profile.service;

import com.neu.CoursePlatform.common.GameEventTypes;
import com.neu.CoursePlatform.common.event.GameEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 画像投影入口。通用事件只用于通知；画像变化必须回查可审计的服务端事实，
 * 不能信任事件 payload 中的数值或事件名称。
 */
@Component
public class ProfileGameEventListener {

    private final ProfileProjectionService projectionService;

    public ProfileGameEventListener(ProfileProjectionService projectionService) {
        this.projectionService = projectionService;
    }

    @EventListener
    public void onGameEvent(GameEvent event) {
        if (event == null || event.getSourceId() == null || event.getEventType() == null) return;
        switch (event.getEventType()) {
            case GameEventTypes.FLOOR_CLEARED, GameEventTypes.FLOOR_FAILED,
                 GameEventTypes.ELITE_DEFEATED, GameEventTypes.BOSS_DEFEATED -> {
                boolean towerApplied = projectionService.applyTowerAttempt(event.getSourceId());
                if (!towerApplied && GameEventTypes.BOSS_DEFEATED.equals(event.getEventType())) {
                    projectionService.applyBossTaskSubmission(event.getSourceId());
                }
            }
            default -> { }
        }
    }
}
