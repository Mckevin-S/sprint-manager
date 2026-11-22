import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { PRIORITY_LABELS, Priority } from '../core/models';

const ICONS: Record<Priority, string> = {
  HIGHEST: 'keyboard_double_arrow_up',
  HIGH: 'keyboard_arrow_up',
  MEDIUM: 'drag_handle',
  LOW: 'keyboard_arrow_down',
  LOWEST: 'keyboard_double_arrow_down',
};

@Component({
  selector: 'app-priority-icon',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<mat-icon [class]="priority().toLowerCase()" [attr.title]="'Priorité ' + labels[priority()].toLowerCase()"
    [attr.aria-label]="'Priorité ' + labels[priority()].toLowerCase()">{{ icons[priority()] }}</mat-icon>`,
  styles: `
    mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .highest { color: var(--danger); }
    .high { color: var(--warning); }
    .medium { color: var(--muted); }
    .low, .lowest { color: var(--info); }
  `,
})
export class PriorityIconComponent {
  readonly priority = input.required<Priority>();
  readonly icons = ICONS;
  readonly labels = PRIORITY_LABELS;
}
