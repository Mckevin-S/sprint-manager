package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "membership")
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole role;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    protected Membership() {
    }

    public Membership(Project project, User user, ProjectRole role, Instant joinedAt) {
        this.project = project;
        this.user = user;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public boolean hasAnyRole(ProjectRole... roles) {
        for (ProjectRole candidate : roles) {
            if (candidate == role) {
                return true;
            }
        }
        return false;
    }

    public void changeRole(ProjectRole role) {
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }

    public ProjectRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
