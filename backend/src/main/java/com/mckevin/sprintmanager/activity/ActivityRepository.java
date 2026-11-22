package com.mckevin.sprintmanager.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<ActivityEvent, Long> {

    @EntityGraph(attributePaths = "actor")
    Page<ActivityEvent> findByProjectIdOrderByOccurredAtDescIdDesc(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = "actor")
    List<ActivityEvent> findByIssueIdOrderByOccurredAtAscIdAsc(Long issueId);

    List<ActivityEvent> findBySprintIdAndRemainingDeltaIsNotNullOrderByOccurredAtAsc(Long sprintId);
}
