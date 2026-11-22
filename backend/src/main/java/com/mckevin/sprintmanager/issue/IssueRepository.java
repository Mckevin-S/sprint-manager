package com.mckevin.sprintmanager.issue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "sprint"})
    Page<Issue> findAll(Specification<Issue> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee", "reporter", "sprint"})
    Optional<Issue> findByIdAndProjectId(Long id, Long projectId);

    @EntityGraph(attributePaths = {"project", "assignee", "sprint"})
    List<Issue> findBySprintIdOrderByRankAscNumberAsc(Long sprintId);

    @EntityGraph(attributePaths = {"project", "sprint"})
    List<Issue> findByProjectIdAndIdIn(Long projectId, Collection<Long> ids);

    @Query("select coalesce(max(i.rank), 0) from Issue i where i.project.id = :projectId")
    long maxRank(@Param("projectId") Long projectId);

    @Query("select count(i) from Issue i where i.project.id = :projectId and i.assignee.id = :userId "
            + "and i.status = com.mckevin.sprintmanager.issue.IssueStatus.IN_PROGRESS and i.id <> :excludedId")
    long countInProgress(@Param("projectId") Long projectId, @Param("userId") Long userId,
                         @Param("excludedId") Long excludedId);

    long countByProjectIdAndStatusNot(Long projectId, IssueStatus status);

    long countBySprintId(Long sprintId);

    /** Statistiques agrégées par sprint, en une seule requête. */
    interface SprintStats {
        Long getSprintId();

        Number getIssueCount();

        Number getTotalPoints();

        Number getDonePoints();
    }

    @Query("select i.sprint.id as sprintId, count(i) as issueCount, "
            + "coalesce(sum(i.storyPoints), 0) as totalPoints, "
            + "coalesce(sum(case when i.status = com.mckevin.sprintmanager.issue.IssueStatus.DONE "
            + "then i.storyPoints else 0 end), 0) as donePoints "
            + "from Issue i where i.project.id = :projectId and i.sprint is not null group by i.sprint.id")
    List<SprintStats> sprintStats(@Param("projectId") Long projectId);

    @EntityGraph(attributePaths = {"project", "assignee", "sprint"})
    List<Issue> findByProjectIdAndSprintIsNullAndStatusNot(Long projectId, IssueStatus status);
}
