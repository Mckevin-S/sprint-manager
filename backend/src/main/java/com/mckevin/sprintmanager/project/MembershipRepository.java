package com.mckevin.sprintmanager.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    @Query("select m from Membership m join fetch m.project join fetch m.user "
            + "where m.project.id = :projectId and m.user.id = :userId")
    Optional<Membership> findMembership(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("select m from Membership m join fetch m.user where m.project.id = :projectId order by m.joinedAt")
    List<Membership> findTeam(@Param("projectId") Long projectId);

    @Query("select m from Membership m join fetch m.project where m.user.id = :userId order by m.project.name")
    List<Membership> findProjectsOf(@Param("userId") Long userId);

    long countByProjectIdAndRole(Long projectId, ProjectRole role);

    long countByProjectId(Long projectId);
}
