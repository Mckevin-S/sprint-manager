# ADR 0001 – Organisation du back-end par fonctionnalité

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le domaine (projets, tickets, sprints, rapports) a des règles riches. Une organisation par couche technique (`controller/`, `service/`, `repository/`) disperse le code d'une même fonctionnalité et rend les dépendances entre fonctionnalités invisibles.

## Décision

Un paquet par fonctionnalité, contenant entité, dépôt, service, contrôleur et DTO. Les règles métier vivent dans les entités ou dans des classes pures sans dépendance à Spring (`IssueWorkflow`, `BurndownCalculator`). Les services orchestrent droits, persistance et journal.

## Conséquences

- Une fonctionnalité se lit et se modifie dans un seul dossier.
- Les règles métier se testent sans démarrer Spring (tests unitaires rapides).
- Une architecture hexagonale stricte (ports et adaptateurs) n'est pas retenue : elle ajouterait des interfaces sans bénéfice à cette taille de projet. Elle reste possible si un second adaptateur de persistance apparaît.
