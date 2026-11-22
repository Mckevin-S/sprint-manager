import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { IssueType, TYPE_LABELS } from '../core/models';

const ICONS: Record<IssueType, string> = { STORY: 'bookmark', BUG: 'bug_report', TASK: 'check_box' };

@Component({
  selector: 'app-issue-type-icon',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="type" [class]="'type ' + type().toLowerCase()" [attr.title]="labels[type()]">
    <mat-icon [attr.aria-label]="labels[type()]">{{ icons[type()] }}</mat-icon></span>`,
  styles: `
    .type { display: inline-flex; width: 20px; height: 20px; border-radius: 5px; align-items: center; justify-content: center; }
    mat-icon { font-size: 14px; width: 14px; height: 14px; color: #fff; }
    .story { background: var(--type-story); }
    .bug { background: var(--type-bug); }
    .task { background: var(--type-task); }
  `,
})
export class IssueTypeIconComponent {
  readonly type = input.required<IssueType>();
  readonly icons = ICONS;
  readonly labels = TYPE_LABELS;
}
