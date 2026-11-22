package com.mckevin.sprintmanager.activity;

import com.mckevin.sprintmanager.user.UserView;

import java.time.Instant;

public record ActivityView(Long id, ActivityType type, String issueKey, String summary, UserView actor, Instant occurredAt) {

    public static ActivityView of(ActivityEvent e) {
        return new ActivityView(e.getId(), e.getType(), e.getIssueKey(), e.getSummary(), UserView.of(e.getActor()),
                e.getOccurredAt());
    }
}
