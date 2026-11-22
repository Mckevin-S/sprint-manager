package com.mckevin.sprintmanager.activity;

import com.mckevin.sprintmanager.issue.Issue;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.user.User;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Point d'entrée unique pour écrire dans le journal d'activité. */
@Component
public class ActivityRecorder {

    private final ActivityRepository repository;

    public ActivityRecorder(ActivityRepository repository) {
        this.repository = repository;
    }

    /** Événement lié à un ticket. Le delta n'est conservé que si le ticket est dans le sprint actif. */
    public void issue(Issue issue, User actor, ActivityType type, String summary, Integer remainingDelta, Instant at) {
        Sprint sprint = issue.getSprint();
        Long sprintId = sprint == null ? null : sprint.getId();
        Integer delta = sprint != null && sprint.isActive() && remainingDelta != null && remainingDelta != 0
                ? remainingDelta : null;
        repository.save(new ActivityEvent(issue.getProject().getId(), issue.getId(), issue.key(), sprintId, actor,
                type, summary, delta, at));
    }

    public void sprint(Sprint sprint, User actor, ActivityType type, String summary, Integer remainingDelta, Instant at) {
        repository.save(new ActivityEvent(sprint.getProject().getId(), null, null, sprint.getId(), actor,
                type, summary, remainingDelta, at));
    }

    public void project(Long projectId, User actor, ActivityType type, String summary, Instant at) {
        repository.save(new ActivityEvent(projectId, null, null, null, actor, type, summary, null, at));
    }
}
