package com.neu.CoursePlatform.service;

import com.neu.CoursePlatform.common.game.GameEventPublisher;
import com.neu.CoursePlatform.common.game.TowerGameEvent;
import com.neu.CoursePlatform.entity.LearningTask;
import com.neu.CoursePlatform.entity.SubmissionAnswer;
import com.neu.CoursePlatform.service.impl.TowerAssessmentEventServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TowerAssessmentEventServiceTest {

    @Mock
    private CourseGameConfigService gameConfigService;

    @Mock
    private GameEventPublisher gameEventPublisher;

    @Test
    void emitsAnswerAndFloorEventsOnlyWhenGameModeIsEnabled() {
        LearningTask task = task();
        SubmissionAnswer answer = answer(true);
        when(gameConfigService.isGameModeEnabled("1")).thenReturn(true);
        TowerAssessmentEventService service = new TowerAssessmentEventServiceImpl(gameConfigService, gameEventPublisher);

        service.publishAssessmentEvents(task, "1001", "8001", List.of(answer));

        ArgumentCaptor<TowerGameEvent> captor = ArgumentCaptor.forClass(TowerGameEvent.class);
        verify(gameEventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        assertEquals(List.of("answer_correct", "floor_cleared"),
                captor.getAllValues().stream().map(TowerGameEvent::eventType).toList());
        assertEquals("10", captor.getAllValues().get(0).payload().get("knowledge_point_id"));
    }

    @Test
    void doesNotEmitAnyEventWhenGameModeIsDisabled() {
        LearningTask task = task();
        when(gameConfigService.isGameModeEnabled("1")).thenReturn(false);
        TowerAssessmentEventService service = new TowerAssessmentEventServiceImpl(gameConfigService, gameEventPublisher);

        service.publishAssessmentEvents(task, "1001", "8001", List.of(answer(false)));

        verify(gameEventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
    }

    private LearningTask task() {
        LearningTask task = new LearningTask();
        task.setCourseCode("1");
        return task;
    }

    private SubmissionAnswer answer(boolean correct) {
        SubmissionAnswer answer = new SubmissionAnswer();
        answer.setQuestionId("100");
        answer.setKnowledgePointId("10");
        answer.setAutoGradable(true);
        answer.setCorrect(correct);
        return answer;
    }
}
