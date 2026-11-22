package com.mckevin.sprintmanager.issue;

import com.mckevin.sprintmanager.shared.BusinessRuleException;

import java.util.List;

/** Estimation en points selon la suite de Fibonacci utilisée en planning poker. */
public final class StoryPoints {

    public static final List<Integer> ALLOWED = List.of(1, 2, 3, 5, 8, 13, 21);

    private StoryPoints() {
    }

    public static Integer validate(Integer points) {
        if (points != null && !ALLOWED.contains(points)) {
            throw new BusinessRuleException("INVALID_STORY_POINTS",
                    "Les points doivent suivre la suite de Fibonacci : " + ALLOWED + ".");
        }
        return points;
    }

    public static int orZero(Integer points) {
        return points == null ? 0 : points;
    }
}
