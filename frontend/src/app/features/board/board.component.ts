import { CdkDrag, CdkDragDrop, CdkDragPlaceholder, CdkDropList, CdkDropListGroup } from '@angular/cdk/drag-drop';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Board, BoardColumn, Issue, IssueStatus, User } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { CloseSprintDialogComponent } from '../backlog/close-sprint-dialog.component';
import { IssueDialogComponent } from '../issue/issue-dialog.component';
import { IssueCardComponent } from './issue-card.component';

/** Tableau Kanban du sprint actif : glisser une carte change son statut via le workflow du back-end. */
@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CdkDropListGroup, CdkDropList, CdkDrag, CdkDragPlaceholder, DatePipe, RouterLink, MatButtonModule, MatButtonToggleModule,
    MatIconModule, MatProgressBarModule, MatTooltipModule, AvatarComponent, EmptyStateComponent, IssueCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './board.component.html',
  styleUrl: './board.component.scss',
})
export class BoardComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly auth = inject(AuthService);
  readonly context = inject(ProjectContext);

  readonly board = signal<Board | null>(null);
  readonly loading = signal(true);
  readonly onlyMine = signal(false);
  readonly assigneeFilter = signal<number | null>(null);

  readonly sprint = computed(() => this.board()?.sprint ?? null);
  readonly progress = computed(() => {
    const s = this.sprint();
    return s && s.totalPoints > 0 ? Math.round((s.donePoints * 100) / s.totalPoints) : 0;
  });
  readonly assignees = computed<User[]>(() => {
    const seen = new Map<number, User>();
    this.board()?.columns.forEach((c) => c.issues.forEach((i) => i.assignee && seen.set(i.assignee.id, i.assignee)));
    return [...seen.values()];
  });

  constructor() {
    effect(() => {
      this.context.revision();
      untracked(() => this.load());
    });
  }

  load(): void {
    this.api.board(this.context.id()).subscribe({
      next: (board) => {
        this.board.set(board);
        this.loading.set(false);
      },
      error: (e) => {
        this.notify.error(e);
        this.loading.set(false);
      },
    });
  }

  visible(column: BoardColumn): Issue[] {
    const me = this.auth.user()?.id;
    const filter = this.onlyMine() ? me : this.assigneeFilter();
    return filter == null ? column.issues : column.issues.filter((i) => i.assignee?.id === filter);
  }

  toggleAssignee(id: number): void {
    this.onlyMine.set(false);
    this.assigneeFilter.update((current) => (current === id ? null : id));
  }

  /** Déplacement optimiste : la carte change de colonne tout de suite, puis revient si le serveur refuse. */
  drop(event: CdkDragDrop<Issue[], Issue[], Issue>, target: IssueStatus): void {
    if (event.previousContainer === event.container) {
      return;
    }
    const issue = event.item.data;
    // Les listes affichées peuvent être filtrées : on déplace par identifiant plutôt que par index.
    const source = event.previousContainer.data;
    source.splice(source.findIndex((i) => i.id === issue.id), 1);
    event.container.data.push({ ...issue, status: target });
    this.api.transition(this.context.id(), issue.id, target).subscribe({
      next: () => this.context.changed(),
      error: (e) => {
        this.notify.error(e);
        this.load();
      },
    });
  }

  open(issue: Issue): void {
    this.dialog.open(IssueDialogComponent, { data: { issueId: issue.id }, width: '960px', maxWidth: '96vw', disableClose: true, autoFocus: 'dialog' });
  }

  closeSprint(): void {
    const sprint = this.sprint();
    if (!sprint) {
      return;
    }
    this.dialog
      .open(CloseSprintDialogComponent, { data: { sprint }, width: '520px' })
      .afterClosed()
      .subscribe((closed) => closed && this.context.changed());
  }

  wipExceeded(column: BoardColumn): boolean {
    if (column.status !== 'IN_PROGRESS') {
      return false;
    }
    const perPerson = new Map<number, number>();
    column.issues.forEach((i) => i.assignee && perPerson.set(i.assignee.id, (perPerson.get(i.assignee.id) ?? 0) + 1));
    const limit = this.board()?.wipLimit ?? Infinity;
    return [...perPerson.values()].some((n) => n >= limit);
  }
}
