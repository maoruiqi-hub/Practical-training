package com.neu.CoursePlatform.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasteryScoreCalculatorTest {
    private final MasteryScoreCalculator calculator = new MasteryScoreCalculator();

    @Test
    void firstCorrectAnswerUsesDifficultyWeight() {
        MasteryScoreCalculator.Calculation result = calculator.calculate(50, 3, 1, true);

        assertEquals(50, result.beforeScore());
        assertEquals(57, result.afterScore());
        assertEquals(new BigDecimal("0.14"), result.alpha());
    }

    @Test
    void firstWrongAnswerDecreasesGradually() {
        MasteryScoreCalculator.Calculation result = calculator.calculate(50, 3, 1, false);

        assertEquals(43, result.afterScore());
    }

    @Test
    void secondAttemptUsesHalfWeight() {
        MasteryScoreCalculator.Calculation result = calculator.calculate(50, 3, 2, true);

        assertEquals(54, result.afterScore());
        assertEquals(0, new BigDecimal("0.07").compareTo(result.alpha()));
    }

    @Test
    void thirdAndLaterAttemptsDoNotChangeMastery() {
        assertEquals(62, calculator.calculate(62, 5, 3, true).afterScore());
        assertEquals(62, calculator.calculate(62, 5, 8, false).afterScore());
    }
}
