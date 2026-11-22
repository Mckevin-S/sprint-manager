import { CdkDrag, CdkDragDrop, CdkDragHandle, CdkDropList, CdkDropListGroup, moveItemInArray } from '@angular/cdk/drag-drop';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Issue, Sprint } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { IssueTypeIconComponent } from '../../shared/issue-type-icon.component';
import { PointsBadgeComponent } from '../../shared/points-badge.component';
import { PriorityIconComponent } from '../../shared/priority-icon.component';
import { StatusChipComponent } from '../../shared/status-chip.component';
import { CreateIssueDialogComponent } from '../issue/create-issue-dialog.component';
import { IssueDialogComponent } from '../issue/issue-dialog.component';
import { StartSprintDialogComponent } from './start-sprint-dialog.component';

const BACKLOG = 'backlog';

/**
 * Backlog et planification : glisser un ticket entre le backlog et un sprint le planifie,
 * le glisser dans une même liste change sa priorité.
 */
@Component({
  selector: 'app-backlog',
  standalone: true,
  imports: [CdkDropListGroup, CdkDropList, CdkDrag, CdkDragHandle, DatePipe, NgTemplateOutlet, FormsModule, RouterLink, MatButtonModule,
    MatIconModule, MatMenuModule, MatProgressBarModule, MatTooltipModule, AvatarComponent, EmptyStateComponent,
    IssueTypeIconComponent, PointsBadgeComponent, PriorityIconComponent, StatusChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './backlog.component.html',
  styleUrl: './backlog.component.scss',
})
export class BacklogComponent {
  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  readonly context = inject(ProjectContext);

  readonly sprints = signal<Sprint[]>([]);
  readonly lists = signal<Record<string, Issue[]>>({});
  readonly loading = signal(true);
  readonly collapsed = signal<Record<string, boolean>>({});
  search = '';

  readonly openSprints = computed(() => this.sprints().filter((s) => s.status !== 'CLOSED'));
  readonly backlog = computed(() => this.lists()[BACKLOG] ?? []);
  readonly backlogPoints = computed(() => this.backlog().reduce((sum, i) => sum + (i.storyPoints ?? 0), 0));
  readonly unestimated = computed(() => this.backlog().filter((i) => i.type === 'STORY' && i.storyPoints == null).length);

  constructor() {
    effect(() => {
      this.context.revision();
      untracked(() => this.load());
    });
  }

  load(): void {
    const projectId = this.context.id();
    this.api.sprints(projectId).subscribe({
      next: (sprints) => {
        this.sprints.set(sprints);
        const open = sprints.filter((s) => s.status !== 'CLOSED');
        forkJoin([
          this.api.issues(projectId, { backlog: true, size: 200 }),
          ...open.map((s) => this.api.issues(projectId, { sprintId: s.id, size: 200 })),
        ]).subscribe({
          next: ([backlog, ...perSprint]) => {
            const lists: Record<string, Issue[]> = { [BACKLOG]: backlog.content };
            open.forEach((s, index) => (lists[String(s.id)] = perSprint[index].content));
            this.lists.set(lists);
            this.loading.set(false);
          },
          error: (e) => this.fail(e),
        });
      },
      error: (e) => this.fail(e),
    });
  }

  listOf(key: string | number): Issue[] {
    return this.lists()[String(key)] ?? [];
  }

  filtered(key: string | number): Issue[] {
    const term = this.search.trim().toLowerCase();
    const list = this.listOf(key);
    return term ? list.filter((i) => i.title.toLowerCase().includes(term) || i.key.toLowerCase().includes(term)) : list;
  }

  points(key: string | number): number {
    return this.listOf(key).reduce((sum, i) => sum + (i.storyPoints ?? 0), 0);
  }

  toggle(key: string | number): void {
    this.collapsed.update((c) => ({ ...c, [String(key)]: !c[String(key)] }));
  }

  /** Glisser-déposer : changement de sprint (planification) ou de position (priorisation). */
  drop(event: CdkDragDrop<string, string, Issue>): void {
    const from = event.previousContainer.data;
    const to = event.container.data;
    const issue = event.item.data;
    const lists = { ...this.lists() };
    const source = [...(lists[from] ?? [])];
    const target = from === to ? source : [...(lists[to] ?? [])];
    const fromIndex = source.findIndex((i) => i.id === issue.id);

    if (from === to) {
      if (this.search.trim()) {
        this.notify.error(new Error('Videz la recherche pour réordonner.'));
        return;
      }
      moveItemInArray(source, fromIndex, event.currentIndex);
      lists[from] = source;
      this.lists.set(lists);
      this.api.rank(this.context.id(), source.map((i) => i.id)).subscribe({ error: (e) => this.rollback(e) });
      return;
    }

    source.splice(fromIndex, 1);
    target.splice(Math.min(event.currentIndex, target.length), 0, issue);
    lists[from] = source;
    lists[to] = target;
    this.lists.set(lists);
    const sprintId = to === BACKLOG ? null : Number(to);
    this.api.plan(this.context.id(), issue.id, sprintId).subscribe({
      next: () => {
        this.api.rank(this.context.id(), target.map((i) => i.id)).subscribe();
        this.refreshSprintStats();
      },
      error: (e) => this.rollback(e),
    });
  }

  createSprint(): void {
    this.api.createSprint(this.context.id(), {}).subscribe({
      next: (sprint) => {
        this.notify.success(`${sprint.name} créé.`);
        this.load();
      },
      error: (e) => this.notify.error(e),
    });
  }

  startSprint(sprint: Sprint): void {
    this.dialog
      .open(StartSprintDialogComponent, { data: { sprint }, width: '520px' })
      .afterClosed()
      .subscribe((started) => {
        if (started) {
          this.notify.success(`${sprint.name} a démarré. Bon sprint !`);
          this.context.changed();
        }
      });
  }

  deleteSprint(sprint: Sprint): void {
    if (!confirm(`Supprimer ${sprint.name} ? Ses tickets retourneront dans le backlog.`)) {
      return;
    }
    this.api.deleteSprint(this.context.id(), sprint.id).subscribe({
      next: () => this.context.changed(),
      error: (e) => this.notify.error(e),
    });
  }

  createIssue(sprintId?: number): void {
    this.dialog
      .open(CreateIssueDialogComponent, { data: { sprintId }, width: '640px' })
      .afterClosed()
      .subscribe((created) => created && this.context.changed());
  }

  open(issue: Issue): void {
    this.dialog.open(IssueDialogComponent, { data: { issueId: issue.id }, width: '960px', maxWidth: '96vw', disableClose: true, autoFocus: 'dialog' });
  }

  private refreshSprintStats(): void {
    this.api.sprints(this.context.id()).subscribe((sprints) => this.sprints.set(sprints));
  }

  private rollback(error: unknown): void {
    this.notify.error(error);
    this.load();
  }

  private fail(error: unknown): void {
    this.notify.error(error);
    this.loading.set(false);
  }
}
