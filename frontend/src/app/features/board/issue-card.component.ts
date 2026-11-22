import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { Issue } from '../../core/models';
import { AvatarComponent } from '../../shared/avatar.component';
import { IssueTypeIconComponent } from '../../shared/issue-type-icon.component';
import { PointsBadgeComponent } from '../../shared/points-badge.component';
import { PriorityIconComponent } from '../../shared/priority-icon.component';

@Component({
  selector: 'app-issue-card',
  standalone: true,
  imports: [AvatarComponent, IssueTypeIconComponent, PriorityIconComponent, PointsBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <article class="card-issue" [class.done]="issue().status === 'DONE'" tabindex="0" role="button"
             [attr.aria-label]="issue().key + ' ' + issue().title" (click)="opened.emit(issue())"
             (keydown.enter)="opened.emit(issue())" [attr.data-testid]="'card-' + issue().key">
      <p class="title">{{ issue().title }}</p>
      <div class="meta">
        <app-issue-type-icon [type]="issue().type" />
        <span class="issue-key">{{ issue().key }}</span>
        <app-priority-icon [priority]="issue().priority" />
        <span class="spacer"></span>
        <app-points-badge [points]="issue().storyPoints" />
        <app-avatar [user]="issue().assignee" [size]="24" />
      </div>
    </article>
  `,
  styles: `
    .card-issue { background: var(--surface); border: 1px solid var(--border); border-radius: 10px; padding: 12px; cursor: pointer;
      box-shadow: var(--shadow-sm); transition: border-color 0.15s, box-shadow 0.15s; outline: none; }
    .card-issue:hover, .card-issue:focus-visible { border-color: var(--border-strong); box-shadow: var(--shadow-md); }
    .card-issue:focus-visible { border-color: var(--accent); }
    .title { margin: 0 0 10px; font-weight: 500; line-height: 1.4; color: var(--ink); }
    .done .title { color: var(--muted); text-decoration: line-through; text-decoration-color: var(--border-strong); }
    .meta { display: flex; align-items: center; gap: 6px; }
  `,
})
export class IssueCardComponent {
  readonly issue = input.required<Issue>();
  readonly opened = output<Issue>();
}
