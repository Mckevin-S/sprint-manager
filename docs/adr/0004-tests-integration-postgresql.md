# ADR 0004 – Tests d'intégration sur PostgreSQL avec Testcontainers

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le schéma utilise des fonctionnalités propres à PostgreSQL (index unique partiel garantissant un seul sprint actif, index sur expression pour l'e-mail). Une base en mémoire (H2) ne les reproduit pas fidèlement.

## Décision

Les tests d'intégration (`*IT`, exécutés par Failsafe) démarrent l'application complète sur un conteneur PostgreSQL 16 partagé entre les classes de test. Chaque test crée ses propres utilisateurs et projets avec des identifiants uniques.

## Conséquences

- Les migrations Flyway et les requêtes sont testées sur le moteur de production.
- Docker est nécessaire pour `mvn verify`, en local comme en CI.
- Les tests restent indépendants de leur ordre d'exécution sans vider la base entre deux tests.
