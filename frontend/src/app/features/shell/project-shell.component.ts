import { ChangeDetectionStrategy, Component, OnDestroy, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ProjectContext } from '../../core/project-context.service';
import { ProjectSummary, ROLE_LABELS } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { LogoComponent } from '../../shared/logo.component';
import { CreateIssueDialogComponent } from '../issue/create-issue-dialog.component';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

/** Cadre d'un projet : navigation latérale, barre supérieure et contenu de la page. */
@Component({
  selector: 'app-project-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatIconModule, MatButtonModule, MatMenuModule, MatTooltipModule,
    MatProgressBarModule, AvatarComponent, LogoComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './project-shell.component.html',
  styleUrl: './project-shell.component.scss',
})
export class ProjectShellComponent implements OnDestroy {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly router = inject(Router);
  readonly context = inject(ProjectContext);
  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);

  readonly projectId = input.required<string>();
  readonly projects = signal<ProjectSummary[]>([]);
  readonly collapsed = signal(false);
  readonly roles = ROLE_LABELS;
  readonly nav: NavItem[] = [
    { path: 'apercu', label: "Vue d'ensemble", icon: 'space_dashboard' },
    { path: 'tableau', label: 'Tableau', icon: 'view_kanban' },
    { path: 'backlog', label: 'Backlog', icon: 'format_list_bulleted' },
    { path: 'rapports', label: 'Rapports', icon: 'insights' },
    { path: 'equipe', label: 'Équipe', icon: 'group' },
    { path: 'activite', label: 'Activité', icon: 'history' },
  ];

  constructor() {
    effect(() => this.load(Number(this.projectId())), { allowSignalWrites: true });
    this.api.projects().subscribe((projects) => this.projects.set(projects));
  }

  ngOnDestroy(): void {
    this.context.project.set(null);
  }

  load(id: number): void {
    this.api.project(id).subscribe({
      next: (project) => this.context.project.set(project),
      error: (e) => {
        this.notify.error(e);
        this.router.navigate(['/projets']);
      },
    });
  }

  createIssue(): void {
    this.dialog
      .open(CreateIssueDialogComponent, { width: '640px' })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.notify.success(`${created.issue.key} créé.`);
          this.context.changed();
        }
      });
  }
}
