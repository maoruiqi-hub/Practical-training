package com.neu.CoursePlatform.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class MasteryScoreCalculator {
    public static final String FORMULA_VERSION = "progressive-v1";
    private static final BigDecimal[] DIFFICULTY_WEIGHTS = {
            BigDecimal.ZERO,
            new BigDecimal("0.10"), new BigDecimal("0.12"), new BigDecimal("0.14"),
            new BigDecimal("0.16"), new BigDecimal("0.18")
    };

    public Calculation calculate(int oldScore, int difficulty, int attemptNo, boolean correct) {
        int safeOld = clamp(oldScore);
        int safeDifficulty = Math.max(1, Math.min(5, difficulty));
        BigDecimal attemptFactor = attemptNo <= 1
                ? BigDecimal.ONE
                : attemptNo == 2 ? new BigDecimal("0.50") : BigDecimal.ZERO;
        BigDecimal alpha = DIFFICULTY_WEIGHTS[safeDifficulty].multiply(attemptFactor);
        int target = correct ? 100 : 0;
        BigDecimal next = BigDecimal.valueOf(safeOld)
                .add(alpha.multiply(BigDecimal.valueOf(target - safeOld)));
        return new Calculation(safeOld, clamp(next.setScale(0, RoundingMode.HALF_UP).intValue()), target, alpha);
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record Calculation(int beforeScore, int afterScore, int targetScore, BigDecimal alpha) {}
}
