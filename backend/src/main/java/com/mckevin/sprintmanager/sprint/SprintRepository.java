package com.mckevin.sprintmanager.sprint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {

    Optional<Sprint> findByIdAndProjectId(Long id, Long projectId);

    Optional<Sprint> findFirstByProjectIdAndStatus(Long projectId, SprintStatus status);

    List<Sprint> findByProjectIdAndStatusInOrderByCreatedAtAsc(Long projectId, List<SprintStatus> statuses);

    List<Sprint> findTop6ByProjectIdAndStatusOrderByClosedAtDesc(Long projectId, SprintStatus status);

    long countByProjectId(Long projectId);

    @Query("select s from Sprint s where s.project.id = :projectId order by s.createdAt desc")
    List<Sprint> findAllOfProject(@Param("projectId") Long projectId);
}
