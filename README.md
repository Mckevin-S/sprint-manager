# Sprint Manager

[![CI](https://github.com/Mckevin-S/sprint-manager/actions/workflows/ci.yml/badge.svg)](https://github.com/Mckevin-S/sprint-manager/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-6db33f)
![Angular 17](https://img.shields.io/badge/Angular-17-dd0031)

Application full stack de pilotage Scrum. Une équipe y gère son backlog, planifie ses sprints et suit l'avancement sur un tableau Kanban. Le burndown et la vélocité sont calculés à partir de l'historique réel des tickets. Les droits suivent les rôles Scrum : le Product Owner rédige et priorise les stories, le Scrum Master pilote les sprints, les développeurs font avancer leurs tickets. Le workflow impose une revue avant qu'un ticket soit terminé et limite le travail en cours de chaque personne.

> Projet personnel de démonstration. L'interface s'appelle « Cadence » ; les données de démonstration sont fictives.

## Fonctionnalités

- **Backlog** priorisé par glisser-déposer, tickets de type story, bug ou tâche, estimation en points Fibonacci.
- **Planification** : glisser un ticket du backlog vers un sprint le planifie.
- **Démarrage de sprint** avec objectif obligatoire et engagement figé. Un seul sprint actif à la fois ; toutes les stories doivent être estimées.
- **Tableau Kanban** du sprint actif : le glisser-déposer applique le workflow côté serveur, et la carte revient à sa place si une règle est enfreinte. Filtres par membre.
- **Workflow** : À faire → En cours → En revue → Terminé, sans pouvoir sauter la revue. Un ticket terminé peut être rouvert.
- **Limite WIP** : nombre maximal de tickets en cours par personne, réglable par projet.
- **Clôture de sprint** : les tickets non terminés repartent « À faire » vers le backlog ou le sprint suivant.
- **Rapports** :
  - burndown jour par jour avec droite idéale ;
  - vélocité des six derniers sprints ;
  - fiabilité de l'engagement ;
  - engagement conseillé pour le sprint suivant.
- **Vue d'ensemble** :
  - avancement du sprint ;
  - répartition par statut ;
  - charge de chaque membre ;
  - santé du backlog ;
  - activité récente.
- **Détail d'un ticket** : édition, assignation, commentaires, historique complet.
- **Équipe** : invitation par e-mail, changement de rôle, matrice des droits. Un projet garde toujours au moins un Product Owner.
- **Journal d'activité** de toutes les actions du projet.
- **Thème clair et sombre**, interface adaptée aux petits écrans.

## Stack

| Couche | Technologies |
|---|---|
| Back-end | Java 21, Spring Boot 3.3 (Web, Validation, Data JPA, Security, OAuth2 Resource Server), Flyway, springdoc-openapi |
| Base de données | PostgreSQL 16 |
| Front-end | Angular 17 (composants autonomes, signaux, nouveau control flow), Angular Material, CDK Drag & Drop, Chart.js |
| Qualité | JUnit 5, AssertJ, MockMvc, Testcontainers, JaCoCo, Jasmine/Karma, ESLint, Playwright |
| Livraison | Docker, Docker Compose, nginx, GitHub Actions |

## Architecture

```
backend/src/main/java/com/mckevin/sprintmanager
├── user/        Comptes, inscription, connexion, jetons JWT
├── project/     Projets, équipe, rôles Scrum, contrôle d'accès
├── issue/       Tickets, workflow, estimation, commentaires
├── sprint/      Planification, démarrage, tableau Kanban, clôture
├── activity/    Journal d'activité (historique et source du burndown)
├── report/      Burndown, vélocité, vue d'ensemble
├── demo/        Données de démonstration
├── config/      Sécurité, OpenAPI, horloge
└── shared/      Exceptions métier, erreurs RFC 7807, pagination

frontend/src/app
├── core/        Modèles, client API typé, authentification, thème, contexte du projet
├── shared/      Composants réutilisables (avatar, icônes, graphique, état vide)
└── features/    Une page par dossier : board, backlog, reports, overview, team...
```

Le code est organisé par fonctionnalité. Les règles métier vivent dans les entités (`Issue.transitionTo`, `Sprint.start`) ou dans des classes pures testables sans Spring (`IssueWorkflow`, `BurndownCalculator`). Les services orchestrent les droits, la persistance et le journal d'activité.

Le burndown est reconstitué à partir du journal d'activité. Chaque événement qui change le travail restant du sprint actif porte sa variation en points : ticket terminé, rouvert, ajouté, retiré ou réestimé. Aucune tâche planifiée ni table d'instantanés n'est nécessaire.

Plus de détails : [docs/architecture.md](docs/architecture.md) et les décisions d'architecture dans [docs/adr](docs/adr).

### Règles métier

1. Seul le Product Owner rédige, modifie et supprime les stories ; tout membre peut créer un bug ou une tâche.
2. Les points suivent la suite de Fibonacci (1, 2, 3, 5, 8, 13, 21).
3. Un ticket passe obligatoirement par la revue avant d'être terminé ; une story doit être estimée pour être terminée.
4. Seuls les tickets du sprint actif peuvent démarrer ; un ticket non assigné est assigné à la personne qui le démarre.
5. Chaque personne a au plus *N* tickets en cours (limite WIP du projet, 3 par défaut).
6. Un développeur ne fait avancer que ses tickets ; le PO et le Scrum Master peuvent tous les déplacer.
7. Un seul sprint actif par projet, garanti aussi par un index unique partiel en base.
8. Démarrer un sprint exige un objectif, au moins un ticket, des stories estimées et une durée de quatre semaines au plus.
9. À la clôture, les points terminés sont figés pour la vélocité ; les tickets non terminés sont reportés « À faire ».
10. Un projet garde toujours au moins un Product Owner.

## API

Documentation interactive : http://localhost:8080/swagger-ui.html (bouton *Authorize* avec le jeton obtenu à la connexion).

| Méthode | Route | Rôle |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Crée un compte ou se connecte, renvoie un jeton JWT |
| GET | `/api/auth/me` | Utilisateur connecté |
| GET / POST | `/api/projects` | Mes projets / créer un projet |
| GET / PUT | `/api/projects/{id}` | Détail et équipe / modifier (nom, limite WIP) |
| POST / PUT / DELETE | `/api/projects/{id}/members[/{membershipId}]` | Ajouter, changer de rôle, retirer un membre |
| GET | `/api/projects/{id}/activity` | Journal d'activité paginé |
| GET / POST | `/api/projects/{id}/issues` | Rechercher (sprint, backlog, statut, type, assigné, texte) / créer |
| GET / PUT / DELETE | `/api/projects/{id}/issues/{issueId}` | Détail avec commentaires et historique / modifier / supprimer |
| POST | `/api/projects/{id}/issues/{issueId}/transition` | Faire avancer dans le workflow |
| PUT | `/api/projects/{id}/issues/{issueId}/sprint` | Planifier dans un sprint ou renvoyer au backlog |
| PUT | `/api/projects/{id}/issues/rank` | Réordonner (priorité) |
| POST | `/api/projects/{id}/issues/{issueId}/comments` | Commenter |
| GET / POST | `/api/projects/{id}/sprints` | Sprints avec statistiques / créer |
| POST | `/api/projects/{id}/sprints/{sprintId}/start` | Démarrer |
| POST | `/api/projects/{id}/sprints/{sprintId}/close` | Clôturer et reporter les tickets non terminés |
| GET | `/api/projects/{id}/board` | Tableau Kanban du sprint actif |
| GET | `/api/projects/{id}/reports/burndown`, `/velocity`, `/overview` | Rapports |

Exemple :

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"mckevin@demo.fr","password":"demo1234"}' | jq -r .token)

curl -X POST http://localhost:8080/api/projects/1/issues/15/transition \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"status":"DONE"}'
```

Les erreurs suivent le format Problem Details (RFC 7807) :

| Code | Cas |
|---|---|
| 400 | Champ invalide |
| 401 | Jeton absent ou expiré |
| 403 | Rôle insuffisant |
| 404 | Ressource absente ou projet dont l'utilisateur n'est pas membre |
| 409 | Doublon ou modification concurrente |
| 422 | Règle métier non respectée, avec un `code` stable (`WIP_LIMIT_REACHED`, `TRANSITION_NOT_ALLOWED`, `UNESTIMATED_STORIES`...) |

## Lancer le projet

### Avec Docker (le plus simple)

```bash
docker compose up --build
```

- Application : http://localhost:4200
- API et Swagger : http://localhost:8080/swagger-ui.html

Les données de démonstration sont chargées au premier démarrage : une équipe de cinq personnes, trois sprints clôturés, un sprint en cours et un backlog. Les boutons « Explorer la démo » de l'écran de connexion connectent directement avec l'un de ces comptes :

| Compte | Rôle |
|---|---|
| `camille@demo.fr` | Product Owner |
| `hugo@demo.fr` | Scrum Master |
| `mckevin@demo.fr`, `ines@demo.fr`, `thomas@demo.fr` | Développeurs |

Mot de passe commun : `demo1234`. Pour démarrer sans données de démonstration : `DEMO_DATA=false docker compose up --build`.

### En développement

Prérequis : Java 21, Maven 3.9, Node 20, Docker (pour PostgreSQL et les tests d'intégration).

```bash
# Base de données
docker compose up -d db

# Back-end (données de démonstration activées)
cd backend
DEMO_DATA=true mvn spring-boot:run

# Front-end (proxy /api → localhost:8080)
cd frontend
npm install
npm start
```

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Connexion PostgreSQL | `jdbc:postgresql://localhost:5432/sprint`, `sprint`, `sprint` |
| `JWT_SECRET` | Clé de signature des jetons (32 caractères minimum) | valeur de développement |
| `JWT_VALIDITY` | Durée de validité des jetons | `PT8H` |
| `CORS_ALLOWED_ORIGINS` | Origines autorisées | `http://localhost:4200` |
| `DEMO_DATA` | Charge les données de démonstration sur une base vide | `false` (`true` avec Docker Compose) |

## Tests

```bash
cd backend && mvn verify                # tests unitaires + tests d'intégration sur PostgreSQL (Docker requis)
cd frontend && npm run lint && npm run test:ci
cd frontend && npm run e2e              # bout en bout, application lancée avec docker compose
```

- **Unitaires (back-end)** :
  - `IssueWorkflowTest`, `IssueTest` : workflow et estimation ;
  - `SprintTest` : démarrage et clôture ;
  - `BurndownCalculatorTest` : calcul jour par jour, fuseau horaire, droite idéale ;
  - `ProjectTest` : clé de projet et limite WIP.
- **Intégration (Testcontainers PostgreSQL)** :
  - `AuthIT` : inscription, connexion, jetons ;
  - `ProjectTeamIT` : équipe, droits, dernier Product Owner ;
  - `SprintLifecycleIT` : parcours Scrum complet, du backlog à la vélocité ;
  - `DemoDataSeederIT` : cohérence des données de démonstration.
- **Front-end** : intercepteur d'authentification, session, droits par rôle, tableau Kanban avec annulation d'un déplacement refusé, écran de connexion, configuration des graphiques.
- **Bout en bout (Playwright)** : parcours Product Owner et développeur sur l'application complète.

La CI GitHub Actions exécute toutes ces suites à chaque push et sur chaque pull request. Le rapport de couverture JaCoCo est publié en artefact.

## Organisation du dépôt

Le développement suit un Git Flow simplifié :
- `main` contient les versions livrées et taguées ;
- `develop` intègre la prochaine version ;
- les branches `feature/SM-<n>-<sujet>`, `fix/...` et `chore/...` partent de `develop` et y reviennent après une CI verte ;
- les messages de commit suivent Conventional Commits.

Le détail figure dans [CONTRIBUTING.md](CONTRIBUTING.md), les versions dans [CHANGELOG.md](CHANGELOG.md).

## Pistes d'évolution

- Notifications en temps réel (WebSocket) quand un coéquipier déplace une carte
- Authentification déléguée à un fournisseur OIDC (Keycloak)
- Sous-tâches et liens entre tickets
- Rapport de flux cumulé et temps de cycle
- Export du backlog en CSV
