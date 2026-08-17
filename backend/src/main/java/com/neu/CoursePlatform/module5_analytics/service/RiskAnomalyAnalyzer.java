package com.neu.CoursePlatform.module5_analytics.service;

import java.util.List;

/**
 * Calculates personal-baseline signals from an ordered score series.
 * The caller owns data access and chooses which historical records belong in a series.
 */
final class RiskAnomalyAnalyzer {

    private RiskAnomalyAnalyzer() {
    }

    static SeriesSignals analyze(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new SeriesSignals(0, null, null, null, 0D, 0D, 0, 0);
        }
        int count = values.size();
        int recentWindow = Math.min(3, Math.max(1, count / 3));
        int recentStart = count - recentWindow;
        Double recentMean = round(mean(values.subList(recentStart, count)));
        Double previousMean = recentStart == 0
                ? null
                : round(mean(values.subList(Math.max(0, recentStart - 3), recentStart)));
        Double delta = previousMean == null ? null : round(recentMean - previousMean);

        double volatility = standardDeviation(values.subList(Math.max(0, count - 6), count));
        double maxDrop = 0D;
        int directionChanges = 0;
        int previousDirection = 0;
        for (int i = 1; i < values.size(); i++) {
            double change = values.get(i) - values.get(i - 1);
            maxDrop = Math.max(maxDrop, -change);
            int direction = Double.compare(change, 0D);
            if (direction != 0 && previousDirection != 0 && direction != previousDirection) {
                directionChanges++;
            }
            if (direction != 0) previousDirection = direction;
        }
        return new SeriesSignals(count, previousMean, recentMean, delta, round(volatility), round(maxDrop),
                directionChanges, values.get(count - 1).intValue());
    }

    private static double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0D);
    }

    private static double standardDeviation(List<Double> values) {
        if (values.size() < 2) return 0D;
        double mean = mean(values);
        double variance = values.stream().mapToDouble(value -> Math.pow(value - mean, 2D)).average().orElse(0D);
        return Math.sqrt(variance);
    }

    private static double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    record SeriesSignals(int evidenceCount, Double previousMean, Double recentMean, Double delta,
                         double volatility, double maxSingleDrop, int directionChanges, int latestValue) {
    }
}
