# ADR 0003 – Authentification par jeton JWT signé, API sans état

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le front-end Angular et l'API sont deux applications distinctes. Il faut une authentification simple à déployer, testable et sans dépendance externe.

## Décision

L'API émet un jeton JWT signé HS256 avec le support natif de Spring Security (`oauth2-resource-server`, `NimbusJwtEncoder`/`NimbusJwtDecoder`). Le sujet du jeton est l'identifiant de l'utilisateur. Les droits fins (rôle dans un projet) sont vérifiés dans les services, car ils dépendent du projet consulté.

## Conséquences

- Aucune session côté serveur : l'API se met à l'échelle horizontalement.
- Pas de cookie, donc pas de protection CSRF à gérer.
- Révocation immédiate impossible avant expiration : la durée de validité est courte (8 h par défaut). Un fournisseur OIDC (Keycloak) pourra remplacer l'émetteur interne sans changer le reste de l'API, puisque seule la validation du jeton est concernée.
