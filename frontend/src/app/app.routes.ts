import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'projets' },
  {
    path: 'connexion',
    canActivate: [guestGuard],
    title: 'Connexion · Cadence',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'inscription',
    canActivate: [guestGuard],
    title: 'Créer un compte · Cadence',
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent),
  },
  {
    path: 'projets',
    canActivate: [authGuard],
    title: 'Projets · Cadence',
    loadComponent: () => import('./features/projects/projects-page.component').then((m) => m.ProjectsPageComponent),
  },
  {
    path: 'projets/:projectId',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/project-shell.component').then((m) => m.ProjectShellComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'apercu' },
      {
        path: 'apercu',
        title: "Vue d'ensemble · Cadence",
        loadComponent: () => import('./features/overview/overview.component').then((m) => m.OverviewComponent),
      },
      {
        path: 'tableau',
        title: 'Tableau · Cadence',
        loadComponent: () => import('./features/board/board.component').then((m) => m.BoardComponent),
      },
      {
        path: 'backlog',
        title: 'Backlog · Cadence',
        loadComponent: () => import('./features/backlog/backlog.component').then((m) => m.BacklogComponent),
      },
      {
        path: 'rapports',
        title: 'Rapports · Cadence',
        loadComponent: () => import('./features/reports/reports.component').then((m) => m.ReportsComponent),
      },
      {
        path: 'equipe',
        title: 'Équipe · Cadence',
        loadComponent: () => import('./features/team/team.component').then((m) => m.TeamComponent),
      },
      {
        path: 'activite',
        title: 'Activité · Cadence',
        loadComponent: () => import('./features/activity/activity.component').then((m) => m.ActivityComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'projets' },
];
