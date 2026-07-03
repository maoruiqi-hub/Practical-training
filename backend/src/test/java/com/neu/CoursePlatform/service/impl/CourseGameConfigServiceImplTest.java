package com.neu.CoursePlatform.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.neu.CoursePlatform.entity.CourseGameConfig;
import com.neu.CoursePlatform.mapper.CourseGameConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CourseGameConfigServiceImplTest {

    private CourseGameConfigMapper mapper;
    private CourseGameConfigServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(CourseGameConfigMapper.class);
        service = new CourseGameConfigServiceImpl(mapper);
    }

    @Test
    void isEnabledOnlyReturnsTrueForExistingEnabledConfig() {
        when(mapper.selectOne(any())).thenReturn(null);
        assertFalse(service.isEnabled("101"));

        CourseGameConfig disabled = new CourseGameConfig();
        disabled.setGameModeEnabled(false);
        when(mapper.selectOne(any())).thenReturn(disabled);
        assertFalse(service.isEnabled("101"));

        CourseGameConfig enabled = new CourseGameConfig();
        enabled.setGameModeEnabled(true);
        when(mapper.selectOne(any())).thenReturn(enabled);
        assertTrue(service.isEnabled("101"));
    }

    @Test
    void updateEnabledInsertsNewConfigWhenMissing() {
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(CourseGameConfig.class))).thenReturn(1);

        assertTrue(service.updateEnabled("101", true));

        ArgumentCaptor<CourseGameConfig> captor = ArgumentCaptor.forClass(CourseGameConfig.class);
        verify(mapper).insert(captor.capture());
        CourseGameConfig saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("101", saved.getCourseId());
        assertTrue(saved.getGameModeEnabled());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void updateEnabledUpdatesExistingConfig() {
        CourseGameConfig existing = new CourseGameConfig();
        existing.setId("cfg-1");
        existing.setCourseId("101");
        existing.setGameModeEnabled(false);
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.updateById(existing)).thenReturn(1);

        assertTrue(service.updateEnabled("101", true));

        assertTrue(existing.getGameModeEnabled());
        assertNotNull(existing.getUpdatedAt());
        verify(mapper).updateById(existing);
        verify(mapper, never()).insert(any(CourseGameConfig.class));
    }
}
