import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, signal, untracked } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../core/api.service';
import { Activity } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { AvatarComponent } from '../../shared/avatar.component';

const ICONS: Record<string, string> = {
  ISSUE_CREATED: 'add_circle',
  ISSUE_UPDATED: 'edit',
  ISSUE_DELETED: 'delete',
  STATUS_CHANGED: 'swap_horiz',
  ASSIGNEE_CHANGED: 'person',
  POINTS_CHANGED: 'casino',
  COMMENT_ADDED: 'chat_bubble',
  SPRINT_CREATED: 'event',
  SPRINT_STARTED: 'play_circle',
  SPRINT_CLOSED: 'task_alt',
  SCOPE_ADDED: 'playlist_add',
  SCOPE_REMOVED: 'playlist_remove',
  MEMBER_ADDED: 'person_add',
  MEMBER_ROLE_CHANGED: 'badge',
  MEMBER_REMOVED: 'person_remove',
};

interface DayGroup {
  day: string;
  items: Activity[];
}

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule, AvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page narrow">
      <div class="page-header">
        <div><h1>Activité</h1><p>Journal complet du projet : chaque action est tracée.</p></div>
      </div>
      @for (group of groups(); track group.day) {
        <h2 class="day">{{ group.day | date: 'EEEE d MMMM yyyy' }}</h2>
        <ol class="timeline card">
          @for (a of group.items; track a.id) {
            <li>
              <span class="icon"><mat-icon>{{ icon(a.type) }}</mat-icon></span>
              <app-avatar [user]="a.actor" [size]="26" />
              <p><strong>{{ a.actor.fullName }}</strong> {{ a.summary }}
                @if (a.issueKey) {
                  <span class="issue-key">{{ a.issueKey }}</span>
                }
              </p>
              <span class="muted time">{{ a.occurredAt | date: 'HH:mm' }}</span>
            </li>
          }
        </ol>
      }
      @if (hasMore()) {
        <div class="more"><button mat-stroked-button (click)="next()">Voir plus</button></div>
      }
    </div>
  `,
  styles: `
    .narrow { max-width: 900px; }
    .day { font-size: 13px; text-transform: capitalize; color: var(--muted); margin: 24px 0 8px; font-weight: 600; }
    .timeline { list-style: none; margin: 0; padding: 4px 16px; }
    .timeline li { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border); }
    .timeline li:last-child { border-bottom: none; }
    .icon { width: 28px; height: 28px; border-radius: 8px; background: var(--surface-2); display: flex; align-items: center;
      justify-content: center; color: var(--muted); flex-shrink: 0; }
    .icon mat-icon { font-size: 16px; width: 16px; height: 16px; }
    p { margin: 0; flex: 1; }
    .issue-key { margin-left: 4px; }
    .time { font-size: 12px; }
    .more { display: flex; justify-content: center; margin-top: 16px; }
  `,
})
export class ActivityComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly context = inject(ProjectContext);

  readonly items = signal<Activity[]>([]);
  readonly groups = signal<DayGroup[]>([]);
  readonly hasMore = signal(false);
  private page = 0;

  constructor() {
    effect(() => {
      this.context.revision();
      untracked(() => {
        this.page = 0;
        this.items.set([]);
        this.fetch();
      });
    });
  }

  next(): void {
    this.page++;
    this.fetch();
  }

  icon(type: string): string {
    return ICONS[type] ?? 'bolt';
  }

  private fetch(): void {
    this.api.activity(this.context.id(), this.page, 40).subscribe({
      next: (page) => {
        const all = [...this.items(), ...page.content];
        this.items.set(all);
        this.hasMore.set(page.page + 1 < page.totalPages);
        const groups = new Map<string, Activity[]>();
        all.forEach((a) => {
          const day = a.occurredAt.slice(0, 10);
          groups.set(day, [...(groups.get(day) ?? []), a]);
        });
        this.groups.set([...groups.entries()].map(([day, items]) => ({ day, items })));
      },
      error: (e) => this.notify.error(e),
    });
  }
}
