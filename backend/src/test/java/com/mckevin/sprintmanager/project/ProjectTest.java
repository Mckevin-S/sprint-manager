package com.mckevin.sprintmanager.project;

import com.mckevin.sprintmanager.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectTest {

    @Test
    void keyIsNormalizedAndIssueNumbersIncrease() {
        Project project = new Project(" orn ", "Orion", null, Instant.EPOCH);

        assertThat(project.getKey()).isEqualTo("ORN");
        assertThat(project.allocateIssueNumber()).isEqualTo(1);
        assertThat(project.allocateIssueNumber()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "1AB", "TOOLONG", "AB-1"})
    void rejectsInvalidKeys(String key) {
        assertThatThrownBy(() -> new Project(key, "Nom", null, Instant.EPOCH))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void wipLimitBounds() {
        Project project = new Project("ORN", "Orion", null, Instant.EPOCH);
        project.changeWipLimit(5);
        assertThat(project.getWipLimit()).isEqualTo(5);
        assertThatThrownBy(() -> project.changeWipLimit(0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> project.changeWipLimit(11)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void membershipRoles() {
        Membership membership = new Membership(null, null, ProjectRole.SCRUM_MASTER, Instant.EPOCH);
        assertThat(membership.hasAnyRole(ProjectRole.PRODUCT_OWNER, ProjectRole.SCRUM_MASTER)).isTrue();
        assertThat(membership.hasAnyRole(ProjectRole.DEVELOPER)).isFalse();
    }
}
