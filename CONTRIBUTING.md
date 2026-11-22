# Contribuer à Sprint Manager

Ce document décrit la façon de travailler sur le projet : branches, commits, revue et qualité.

## Branches (Git Flow simplifié)

| Branche | Rôle | Règle |
|---|---|---|
| `main` | Code livré, chaque version est taguée (`v1.0.0`) | Jamais de commit direct ; uniquement des merges depuis `release/*` ou `hotfix/*` |
| `develop` | Intégration de la prochaine version | Reçoit les branches de travail après revue et CI verte |
| `feature/SM-<n>-<sujet>` | Une fonctionnalité | Créée depuis `develop`, fusionnée dans `develop` |
| `fix/<sujet>` | Une correction sur `develop` | Idem |
| `chore/<sujet>`, `docs/<sujet>` | Outillage, configuration, documentation | Idem |
| `release/<version>` | Stabilisation d'une version | Créée depuis `develop`, fusionnée dans `main` (tag) puis dans `develop` |
| `hotfix/<version>` | Correction urgente en production | Créée depuis `main`, fusionnée dans `main` et `develop` |

Les noms de branches sont en minuscules, avec des tirets : `feature/SM-4-sprints`, `fix/testcontainers-docker29`.

Les fusions se font sans avance rapide (`--no-ff`) pour garder la trace de chaque branche dans l'historique.

## Messages de commit (Conventional Commits)

```
<type>(<portée>): <description à l'impératif, en minuscules>

<corps facultatif : le pourquoi, pas le comment>
```

| Type | Usage |
|---|---|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction d'anomalie |
| `refactor` | Restructuration sans changement de comportement |
| `test` | Ajout ou correction de tests |
| `docs` | Documentation |
| `build` | Dépendances, Maven, npm |
| `ci` | Intégration continue |
| `chore` | Maintenance diverse |

Portées utilisées : `auth`, `projets`, `tickets`, `sprints`, `rapports`, `activite`, `demo`, `api`, `db`, `front`, `board`, `backlog`, `e2e`.

Exemples :

```
feat(sprints): clôture avec report des tickets non terminés
fix(board): annule le déplacement si le serveur refuse la transition
test(rapports): burndown sur le fuseau Europe/Paris
```

## Pull requests

1. Une PR = un sujet. Elle cible `develop`.
2. La CI doit être verte : build, tests unitaires et d'intégration, lint, tests de bout en bout.
3. Le modèle de PR (`.github/pull_request_template.md`) sert de liste de contrôle.
4. Toute nouvelle règle métier arrive avec ses tests.

## Qualité du code

### Back-end

- Organisation par fonctionnalité (`project`, `issue`, `sprint`, `report`...) plutôt que par couche technique.
- Règles métier dans les entités (`Issue.transitionTo`, `Sprint.start`) ou dans des classes pures (`IssueWorkflow`, `BurndownCalculator`), faciles à tester sans Spring.
- Les services orchestrent : droits, chargement, persistance, journal d'activité.
- Erreurs métier : `BusinessRuleException` avec un code stable (`WIP_LIMIT_REACHED`...), traduites en réponses RFC 7807.
- Tests d'intégration sur une vraie base PostgreSQL (Testcontainers), suffixe `IT`.

### Front-end

- Composants autonomes (`standalone`), signaux, `ChangeDetectionStrategy.OnPush`.
- Un seul client HTTP typé (`ApiService`) ; aucun appel HTTP dans les gabarits.
- Les droits affichés reprennent ceux de l'API (`ProjectContext`), mais la vérification reste côté serveur.
- `npm run lint` doit passer sans avertissement.

## Commandes utiles

```bash
# Back-end
cd backend && mvn verify                 # tests unitaires + intégration (Docker requis)

# Front-end
cd frontend && npm run lint && npm run test:ci && npm run build

# Bout en bout (application lancée avec docker compose)
cd frontend && npx playwright install chromium && npm run e2e
```
