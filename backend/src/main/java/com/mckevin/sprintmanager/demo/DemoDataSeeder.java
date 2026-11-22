package com.mckevin.sprintmanager.demo;

import com.mckevin.sprintmanager.activity.ActivityEvent;
import com.mckevin.sprintmanager.activity.ActivityRepository;
import com.mckevin.sprintmanager.activity.ActivityType;
import com.mckevin.sprintmanager.issue.Comment;
import com.mckevin.sprintmanager.issue.CommentRepository;
import com.mckevin.sprintmanager.issue.Issue;
import com.mckevin.sprintmanager.issue.IssueRepository;
import com.mckevin.sprintmanager.issue.IssueStatus;
import com.mckevin.sprintmanager.issue.IssueType;
import com.mckevin.sprintmanager.issue.Priority;
import com.mckevin.sprintmanager.project.Membership;
import com.mckevin.sprintmanager.project.MembershipRepository;
import com.mckevin.sprintmanager.project.Project;
import com.mckevin.sprintmanager.project.ProjectRepository;
import com.mckevin.sprintmanager.project.ProjectRole;
import com.mckevin.sprintmanager.shared.AppTime;
import com.mckevin.sprintmanager.sprint.Sprint;
import com.mckevin.sprintmanager.sprint.SprintRepository;
import com.mckevin.sprintmanager.user.User;
import com.mckevin.sprintmanager.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Jeu de données de démonstration réaliste : une équipe de cinq personnes, trois sprints clôturés
 * (pour la vélocité), un sprint en cours depuis six jours (pour le burndown), un sprint planifié et un backlog.
 * Activé par DEMO_DATA=true, uniquement sur une base vide.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "demo1234";
    private static final Logger LOG = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final UserRepository users;
    private final ProjectRepository projects;
    private final MembershipRepository memberships;
    private final SprintRepository sprints;
    private final IssueRepository issues;
    private final CommentRepository comments;
    private final ActivityRepository activities;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    private Project project;
    private long rank;
    private LocalDate today;

    @SuppressWarnings("java:S107")
    public DemoDataSeeder(UserRepository users, ProjectRepository projects, MembershipRepository memberships,
                          SprintRepository sprints, IssueRepository issues, CommentRepository comments,
                          ActivityRepository activities, PasswordEncoder passwordEncoder, Clock clock) {
        this.users = users;
        this.projects = projects;
        this.memberships = memberships;
        this.sprints = sprints;
        this.issues = issues;
        this.comments = comments;
        this.activities = activities;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            LOG.info("Données de démonstration ignorées : la base contient déjà des utilisateurs.");
            return;
        }
        today = LocalDate.now(clock.withZone(AppTime.ZONE));
        String hash = passwordEncoder.encode(DEMO_PASSWORD);
        Instant origin = at(-70, 9);
        User camille = users.save(new User("camille@demo.fr", "Camille Durand", hash, origin));
        User hugo = users.save(new User("hugo@demo.fr", "Hugo Lefèvre", hash, origin));
        User mckevin = users.save(new User("mckevin@demo.fr", "Mckevin Kamguem", hash, origin));
        User ines = users.save(new User("ines@demo.fr", "Inès Moreau", hash, origin));
        User thomas = users.save(new User("thomas@demo.fr", "Thomas Petit", hash, origin));

        project = projects.save(new Project("ORN", "Orion – Portail client",
                "Refonte du portail client d'un assureur : espace adhérent, déclaration de sinistre en ligne, "
                        + "suivi des dossiers et notifications.", origin));
        memberships.save(new Membership(project, camille, ProjectRole.PRODUCT_OWNER, origin));
        memberships.save(new Membership(project, hugo, ProjectRole.SCRUM_MASTER, origin));
        memberships.save(new Membership(project, mckevin, ProjectRole.DEVELOPER, origin));
        memberships.save(new Membership(project, ines, ProjectRole.DEVELOPER, origin));
        memberships.save(new Membership(project, thomas, ProjectRole.DEVELOPER, origin));

        closedSprint(1, -62, 23, camille, hugo, List.of(
                story("Connexion à l'espace adhérent", 5, mckevin),
                story("Création de compte avec vérification d'e-mail", 8, ines),
                task("Mise en place de la CI et des environnements", 3, thomas),
                story("Page d'accueil de l'espace adhérent", 5, mckevin)));
        closedSprint(2, -48, 28, camille, hugo, List.of(
                story("Consultation des contrats", 8, ines),
                story("Téléchargement des attestations en PDF", 5, thomas),
                bug("Session expirée sans message clair", 2, mckevin),
                story("Modification des coordonnées", 5, mckevin),
                task("Tests de charge de l'API contrats", 3, thomas),
                bug("Accents mal affichés dans les attestations", 1, ines)));
        closedSprint(3, -34, 26, camille, hugo, List.of(
                story("Déclaration de sinistre : étape 1, circonstances", 8, mckevin),
                story("Déclaration de sinistre : ajout de photos", 5, ines),
                story("Récapitulatif et envoi de la déclaration", 5, thomas),
                task("Stockage des pièces jointes chiffré", 3, ines)));

        activeSprint(camille, hugo, mckevin, ines, thomas);
        plannedSprintAndBacklog(camille, hugo, mckevin, ines);
        LOG.info("Données de démonstration créées : connectez-vous avec camille@demo.fr / {}", DEMO_PASSWORD);
    }

    private record Draft(IssueType type, String title, Integer points, User assignee) {
    }

    private static Draft story(String title, Integer points, User assignee) {
        return new Draft(IssueType.STORY, title, points, assignee);
    }

    private static Draft bug(String title, Integer points, User assignee) {
        return new Draft(IssueType.BUG, title, points, assignee);
    }

    private static Draft task(String title, Integer points, User assignee) {
        return new Draft(IssueType.TASK, title, points, assignee);
    }

    /** Sprint de 14 jours commençant au jour relatif {@code startOffset}, entièrement réalisé sauf un écart d'engagement. */
    private void closedSprint(int index, int startOffset, int committed, User po, User sm, List<Draft> drafts) {
        Sprint sprint = sprints.save(new Sprint(project, "Sprint " + index, null, at(startOffset - 2, 10)));
        List<Issue> created = drafts.stream().map(d -> create(d, po, startOffset - 3, sprint)).toList();
        sprint.start(goalOf(index), day(startOffset), day(startOffset + 14), committed, at(startOffset, 9));
        sprintEvent(sprint, sm, ActivityType.SPRINT_STARTED, "a démarré " + sprint.getName(), startOffset);
        int completed = 0;
        for (int i = 0; i < created.size(); i++) {
            Issue issue = created.get(i);
            int doneDay = startOffset + 3 + i * 2;
            moveToDone(issue, doneDay, sprint);
            completed += issue.points();
        }
        sprint.close(completed, at(startOffset + 14, 17));
        sprintEvent(sprint, sm, ActivityType.SPRINT_CLOSED,
                "a clôturé " + sprint.getName() + " : " + completed + " / " + committed + " points terminés", startOffset + 14);
    }

    private void activeSprint(User po, User sm, User mckevin, User ines, User thomas) {
        int start = -6;
        Sprint sprint = sprints.save(new Sprint(project, "Sprint 4", null, at(start - 2, 10)));
        Issue suivi = create(story("Suivi en temps réel d'un dossier sinistre", 8, mckevin), po, start - 3, sprint);
        Issue notif = create(story("Notifications e-mail à chaque étape du dossier", 5, ines), po, start - 3, sprint);
        Issue rdv = create(story("Prise de rendez-vous avec un expert", 8, thomas), po, start - 3, sprint);
        Issue docs = create(story("Dépôt de documents complémentaires", 3, mckevin), po, start - 3, sprint);
        Issue fil = create(story("Fil de messages avec le gestionnaire", 5, ines), po, start - 3, sprint);
        Issue a11y = create(task("Audit d'accessibilité RGAA des formulaires", 3, thomas), po, start - 3, sprint);
        Issue pagination = create(bug("La liste des dossiers ne se pagine pas au-delà de 50", 2, mckevin), po, start - 3, sprint);
        Issue export = create(story("Export de l'historique du dossier en PDF", 3, null), po, start - 3, sprint);
        Issue faq = create(task("Rédiger la FAQ sinistres", 1, null), po, start - 3, sprint);

        int committed = List.of(suivi, notif, rdv, docs, fil, a11y, pagination, export, faq).stream()
                .mapToInt(Issue::points).sum();
        sprint.start("Permettre à l'adhérent de suivre son dossier sinistre de bout en bout", day(start), day(start + 14),
                committed, at(start, 9));
        sprintEvent(sprint, sm, ActivityType.SPRINT_STARTED, "a démarré Sprint 4 (" + committed + " points engagés)", start);

        moveToDone(pagination, start + 1, sprint);
        moveToDone(docs, start + 2, sprint);
        Issue hotfix = create(bug("Erreur 500 à l'envoi d'une photo HEIC", 2, ines), sm, start + 3, null);
        hotfix.planInto(sprint, at(start + 3, 11));
        recordIssue(hotfix, sm, ActivityType.SCOPE_ADDED, "a planifié le ticket dans Sprint 4", 2, start + 3, sprint);
        moveToDone(hotfix, start + 4, sprint);
        moveToDone(notif, start + 5, sprint);

        advance(suivi, IssueStatus.IN_REVIEW, start + 4);
        advance(a11y, IssueStatus.IN_REVIEW, start + 5);
        advance(fil, IssueStatus.IN_PROGRESS, start + 4);
        advance(rdv, IssueStatus.IN_PROGRESS, start + 5);
        advance(export, IssueStatus.TODO, start);
        advance(faq, IssueStatus.TODO, start);

        comments.save(new Comment(suivi, ines, "La maquette du fil d'étapes est validée par l'UX, je reprends les libellés.", at(start + 3, 14)));
        comments.save(new Comment(suivi, po, "Pensez à afficher la date prévisionnelle de la prochaine étape.", at(start + 4, 9)));
        comments.save(new Comment(rdv, thomas, "Le créneau doit-il être confirmé par l'expert ? À valider avec Camille.", at(start + 5, 16)));
    }

    private void plannedSprintAndBacklog(User po, User sm, User mckevin, User ines) {
        Sprint next = sprints.save(new Sprint(project, "Sprint 5", "Ouvrir l'espace adhérent sur mobile", at(-1, 15)));
        create(story("Application mobile : écran de connexion", 5, null), po, -1, next);
        create(story("Application mobile : liste des dossiers", 8, null), po, -1, next);
        create(task("Notifications push (étude technique)", 3, mckevin), po, -1, next);

        create(story("Paiement en ligne de la franchise", 13, null), po, -2, null);
        create(story("Signature électronique des documents", 8, null), po, -2, null);
        create(story("Tableau de bord gestionnaire", null, null), po, -2, null);
        create(bug("Le bouton « Retour » perd les données saisies", 2, ines), sm, -1, null);
        create(story("Chat en ligne avec un conseiller", null, null), po, -1, null);
        create(task("Mise à jour vers Angular 18", 3, null), sm, -1, null);
        create(story("Parrainage d'un proche", 5, null), po, 0, null);
    }

    private Issue create(Draft draft, User reporter, int dayOffset, Sprint sprint) {
        Instant when = at(dayOffset, 10);
        rank += 1_000;
        Issue issue = new Issue(project, project.allocateIssueNumber(), draft.type(), draft.title(),
                describe(draft), priorityOf(draft), draft.points(), reporter, rank, when);
        if (draft.assignee() != null) {
            issue.assignTo(draft.assignee(), when);
        }
        if (sprint != null) {
            issue.planInto(sprint, when);
        }
        issues.save(issue);
        activities.save(new ActivityEvent(project.getId(), issue.getId(), issue.key(), sprint == null ? null : sprint.getId(),
                reporter, ActivityType.ISSUE_CREATED, "a créé « " + issue.getTitle() + " » (" + issue.getType().label() + ")",
                null, when));
        return issue;
    }

    /** Fait parcourir le workflow complet au ticket ; le passage à « Terminé » réduit le travail restant. */
    private void moveToDone(Issue issue, int doneDay, Sprint sprint) {
        User actor = issue.getAssignee() != null ? issue.getAssignee() : issue.getReporter();
        step(issue, actor, IssueStatus.IN_PROGRESS, at(doneDay - 1, 9), sprint, null);
        step(issue, actor, IssueStatus.IN_REVIEW, at(doneDay, 11), sprint, null);
        step(issue, actor, IssueStatus.DONE, at(doneDay, 16), sprint, -issue.points());
    }

    private void advance(Issue issue, IssueStatus target, int dayOffset) {
        User actor = issue.getAssignee() != null ? issue.getAssignee() : issue.getReporter();
        if (target == IssueStatus.TODO) {
            return;
        }
        step(issue, actor, IssueStatus.IN_PROGRESS, at(dayOffset - 1, 9), issue.getSprint(), null);
        if (target == IssueStatus.IN_REVIEW) {
            step(issue, actor, IssueStatus.IN_REVIEW, at(dayOffset, 15), issue.getSprint(), null);
        }
    }

    private void step(Issue issue, User actor, IssueStatus target, Instant when, Sprint sprint, Integer delta) {
        IssueStatus from = issue.getStatus();
        issue.transitionTo(target, when);
        activities.save(new ActivityEvent(project.getId(), issue.getId(), issue.key(), sprint == null ? null : sprint.getId(),
                actor, ActivityType.STATUS_CHANGED, "a déplacé de « " + from.label() + " » vers « " + target.label() + " »",
                delta, when));
    }

    private void recordIssue(Issue issue, User actor, ActivityType type, String summary, Integer delta, int dayOffset,
                             Sprint sprint) {
        activities.save(new ActivityEvent(project.getId(), issue.getId(), issue.key(), sprint.getId(), actor, type, summary,
                delta, at(dayOffset, 11)));
    }

    private void sprintEvent(Sprint sprint, User actor, ActivityType type, String summary, int dayOffset) {
        activities.save(new ActivityEvent(project.getId(), null, null, sprint.getId(), actor, type, summary, null,
                at(dayOffset, type == ActivityType.SPRINT_CLOSED ? 17 : 9)));
    }

    private static String goalOf(int index) {
        return switch (index) {
            case 1 -> "Permettre à l'adhérent de se connecter et d'accéder à son espace";
            case 2 -> "Donner accès aux contrats et aux attestations";
            default -> "Déclarer un sinistre en ligne, photos comprises";
        };
    }

    private static Priority priorityOf(Draft draft) {
        if (draft.type() == IssueType.BUG) {
            return Priority.HIGH;
        }
        return draft.points() != null && draft.points() >= 8 ? Priority.HIGH : Priority.MEDIUM;
    }

    private static String describe(Draft draft) {
        if (draft.type() != IssueType.STORY) {
            return null;
        }
        return "En tant qu'adhérent, je veux : " + draft.title().toLowerCase() + ".\n\n"
                + "Critères d'acceptation :\n- le parcours fonctionne sur ordinateur et mobile ;\n"
                + "- les erreurs sont expliquées en langage clair ;\n- l'action est tracée dans l'historique du dossier.";
    }

    private LocalDate day(int offset) {
        return today.plusDays(offset);
    }

    private Instant at(int dayOffset, int hour) {
        return today.plusDays(dayOffset).atTime(LocalTime.of(hour, 0)).atZone(AppTime.ZONE).toInstant();
    }
}
