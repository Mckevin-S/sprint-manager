# ADR 0002 – Calculer le burndown à partir du journal d'activité

- Statut : accepté
- Date : 2026-11-22

## Contexte

Le burndown a besoin du travail restant pour chaque jour du sprint. Deux options : enregistrer un instantané quotidien (tâche planifiée), ou reconstituer l'historique à partir des événements.

## Décision

Chaque événement qui modifie le travail restant du sprint actif porte un `remainingDelta`. Le burndown est une fonction pure de l'engagement initial et de ces variations (`BurndownCalculator`).

## Conséquences

- Pas de tâche planifiée ni de table d'instantanés ; l'historique est exact à la minute près.
- Les ajouts de périmètre en cours de sprint apparaissent sur la courbe, comme dans les outils du marché.
- Le même journal alimente l'historique des tickets et le fil d'activité du projet.
- Toute nouvelle action qui modifie le périmètre doit renseigner le delta : c'est centralisé dans `ActivityRecorder`, qui ignore le delta hors sprint actif.
