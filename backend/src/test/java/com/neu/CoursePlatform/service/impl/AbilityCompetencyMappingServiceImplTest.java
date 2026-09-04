package com.neu.CoursePlatform.service.impl;

import com.neu.CoursePlatform.mapper.AbilityPointCompetencyRelationMapper;
import com.neu.CoursePlatform.mapper.CompetencyPointMapper;
import com.neu.CoursePlatform.mapper.CompetencyTaskObservationMapper;
import com.neu.CoursePlatform.service.AbilityPointService;
import com.neu.CoursePlatform.service.AbilitySnapshotService;
import com.neu.CoursePlatform.service.LearningTaskService;
import com.neu.CoursePlatform.service.TaskSubmissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbilityCompetencyMappingServiceImplTest {
    @Mock private AbilityPointService abilityPointService;
    @Mock private CompetencyPointMapper competencyMapper;
    @Mock private AbilityPointCompetencyRelationMapper relationMapper;
    @Mock private CompetencyTaskObservationMapper observationMapper;
    @Mock private LearningTaskService taskService;
    @Mock private TaskSubmissionService submissionService;
    @Mock private AbilitySnapshotService abilitySnapshotService;
    @Mock private JdbcTemplate jdbcTemplate;

    private AbilityCompetencyMappingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AbilityCompetencyMappingServiceImpl(abilityPointService, competencyMapper, relationMapper,
                observationMapper, taskService, submissionService, abilitySnapshotService, jdbcTemplate);
    }

    @Test
    void noPublishedVersionUsesInitialVersionOnlyWhenQueryHasNoRows() {
        when(abilityPointService.listByCourseCode("c-1")).thenReturn(List.of());
        when(competencyMapper.selectList(any())).thenReturn(List.of());
        when(relationMapper.selectList(any())).thenReturn(List.of());
        when(observationMapper.selectList(any())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Object[].class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        assertEquals("v1", service.getByCourseCode("c-1").getMatrixVersion());
    }

    @Test
    void databaseFailureIsNotSilentlyConvertedToInitialVersion() {
        when(jdbcTemplate.queryForObject(any(String.class), any(Class.class), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        assertThrows(DataAccessResourceFailureException.class, () -> service.getByCourseCode("c-1"));
    }
}
