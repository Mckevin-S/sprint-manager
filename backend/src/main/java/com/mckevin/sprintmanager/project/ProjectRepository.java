package com.mckevin.sprintmanager.project;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByProjectKey(String projectKey);
}
