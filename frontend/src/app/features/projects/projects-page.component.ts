import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectSummary, ROLE_LABELS } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { LogoComponent } from '../../shared/logo.component';
import { CreateProjectDialogComponent } from './create-project-dialog.component';

@Component({
  selector: 'app-projects-page',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, MatTooltipModule,
    AvatarComponent, EmptyStateComponent, LogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="topbar">
      <app-logo />
      <span class="spacer"></span>
      <button mat-icon-button (click)="theme.toggle()" [matTooltip]="theme.theme() === 'dark' ? 'Thème clair' : 'Thème sombre'"
              aria-label="Changer de thème">
        <mat-icon>{{ theme.theme() === 'dark' ? 'light_mode' : 'dark_mode' }}</mat-icon>
      </button>
      <button class="me" [matMenuTriggerFor]="menu" aria-label="Mon compte">
        <app-avatar [user]="auth.user()" [size]="32" />
      </button>
      <mat-menu #menu="matMenu">
        <div class="menu-user">{{ auth.user()?.fullName }}<br /><span class="muted">{{ auth.user()?.email }}</span></div>
        <button mat-menu-item (click)="auth.logout()"><mat-icon>logout</mat-icon>Se déconnecter</button>
      </mat-menu>
    </header>
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }

    <main class="page">
      <div class="page-header">
        <div>
          <h1>Bonjour {{ firstName() }} 👋</h1>
          <p>Vos projets et l'avancement des sprints en cours.</p>
        </div>
        <button mat-flat-button color="primary" (click)="create()"><mat-icon>add</mat-icon> Nouveau projet</button>
      </div>

      @if (!loading() && projects().length === 0) {
        <div class="card">
          <app-empty-state icon="rocket_launch" title="Aucun projet pour l'instant"
                           message="Créez votre premier projet ou demandez à un Product Owner de vous ajouter à son équipe.">
            <button mat-flat-button color="primary" (click)="create()">Créer un projet</button>
          </app-empty-state>
        </div>
      }

      <div class="grid">
        @for (project of projects(); track project.id) {
          <a class="project card" [routerLink]="['/projets', project.id]" [attr.data-testid]="'project-' + project.key">
            <div class="head">
              <span class="key">{{ project.key }}</span>
              <span class="role">{{ roles[project.myRole] }}</span>
            </div>
            <h2>{{ project.name }}</h2>
            <p class="desc">{{ project.description || 'Pas de description.' }}</p>
            <div class="stats">
              <span><mat-icon>bolt</mat-icon>{{ project.activeSprint ?? 'Aucun sprint actif' }}</span>
              <span><mat-icon>assignment</mat-icon>{{ project.openIssues }} ouverts</span>
              <span><mat-icon>group</mat-icon>{{ project.memberCount }}</span>
            </div>
          </a>
        }
      </div>
    </main>
  `,
  styles: `
    .topbar { display: flex; align-items: center; gap: 8px; padding: 12px 32px; background: var(--surface);
      border-bottom: 1px solid var(--border); position: sticky; top: 0; z-index: 10; }
    .me { border: none; background: none; padding: 0; cursor: pointer; border-radius: 50%; }
    .menu-user { padding: 8px 16px 12px; font-weight: 600; font-size: 13px; }
    .page-header h1 { font-size: 28px; }
    .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
    .project { display: flex; flex-direction: column; gap: 8px; padding: 20px; text-decoration: none; color: inherit;
      transition: transform 0.15s, box-shadow 0.15s, border-color 0.15s; }
    .project:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); border-color: var(--border-strong); }
    .head { display: flex; justify-content: space-between; align-items: center; }
    .key { font-weight: 700; font-size: 12px; letter-spacing: 0.08em; background: var(--accent-soft); color: var(--accent);
      padding: 3px 8px; border-radius: 6px; }
    .role { font-size: 12px; color: var(--muted); }
    h2 { margin: 4px 0 0; font-size: 18px; }
    .desc { margin: 0; color: var(--muted); display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
      overflow: hidden; min-height: 42px; }
    .stats { display: flex; gap: 16px; margin-top: 8px; padding-top: 12px; border-top: 1px solid var(--border);
      font-size: 12px; color: var(--ink-soft); flex-wrap: wrap; }
    .stats span { display: inline-flex; align-items: center; gap: 4px; }
    .stats mat-icon { font-size: 16px; width: 16px; height: 16px; color: var(--muted); }
  `,
})
export class ProjectsPageComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly router = inject(Router);
  private readonly notify = inject(NotifyService);
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);

  readonly projects = signal<ProjectSummary[]>([]);
  readonly loading = signal(true);
  readonly roles = ROLE_LABELS;

  ngOnInit(): void {
    this.api.projects().subscribe({
      next: (projects) => {
        this.projects.set(projects);
        this.loading.set(false);
      },
      error: (e) => {
        this.notify.error(e);
        this.loading.set(false);
      },
    });
  }

  firstName(): string {
    return this.auth.user()?.fullName.split(' ')[0] ?? '';
  }

  create(): void {
    this.dialog
      .open(CreateProjectDialogComponent, { width: '560px' })
      .afterClosed()
      .subscribe((project) => {
        if (project) {
          this.notify.success(`Projet ${project.key} créé.`);
          this.router.navigate(['/projets', project.id]);
        }
      });
  }
}
