package com.neu.CoursePlatform.module5_analytics.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskAnomalyAnalyzerTest {

    @Test
    void latestSuddenDropUsesEarlierScoresAsPersonalBaseline() {
        RiskAnomalyAnalyzer.SeriesSignals signals = RiskAnomalyAnalyzer.analyze(List.of(82D, 79D, 81D, 46D));

        assertEquals(4, signals.evidenceCount());
        assertEquals(80.67D, signals.previousMean());
        assertEquals(46D, signals.recentMean());
        assertEquals(-34.67D, signals.delta());
        assertEquals(35D, signals.maxSingleDrop());
    }

    @Test
    void alternatingScoresExposeVolatilityAndDirectionChanges() {
        RiskAnomalyAnalyzer.SeriesSignals signals = RiskAnomalyAnalyzer.analyze(List.of(50D, 90D, 48D, 92D, 45D));

        assertEquals(5, signals.evidenceCount());
        assertTrue(signals.volatility() >= 18D);
        assertEquals(3, signals.directionChanges());
        assertEquals(47D, signals.maxSingleDrop());
    }

    @Test
    void shortSeriesDoesNotInventABaseline() {
        RiskAnomalyAnalyzer.SeriesSignals signals = RiskAnomalyAnalyzer.analyze(List.of(62D));

        assertEquals(1, signals.evidenceCount());
        assertEquals(null, signals.previousMean());
        assertEquals(null, signals.delta());
        assertEquals(0D, signals.maxSingleDrop());
    }
}
