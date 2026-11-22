package com.mckevin.sprintmanager.issue;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByIssueIdOrderByCreatedAtAsc(Long issueId);

    long countByIssueId(Long issueId);
}
