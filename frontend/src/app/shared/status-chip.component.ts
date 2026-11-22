import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { IssueStatus, STATUS_LABELS } from '../core/models';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="chip" [class]="'chip ' + status().toLowerCase()">{{ labels[status()] }}</span>`,
  styles: `
    .chip { display: inline-flex; align-items: center; padding: 2px 8px; border-radius: 6px; font-size: 11px;
      font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; white-space: nowrap; }
    .todo { background: var(--status-todo-bg); color: var(--status-todo); }
    .in_progress { background: var(--status-progress-bg); color: var(--status-progress); }
    .in_review { background: var(--status-review-bg); color: var(--status-review); }
    .done { background: var(--status-done-bg); color: var(--status-done); }
  `,
})
export class StatusChipComponent {
  readonly status = input.required<IssueStatus>();
  readonly labels = STATUS_LABELS;
}
