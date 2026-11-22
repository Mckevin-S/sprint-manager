import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-points-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="points" [class.unset]="(points() ?? null) === null"
    [attr.title]="(points() ?? null) === null ? 'Non estimé' : points() + ' points'">{{ points() ?? '–' }}</span>`,
  styles: `
    .points { display: inline-flex; min-width: 22px; height: 20px; padding: 0 6px; border-radius: 10px; align-items: center;
      justify-content: center; font-size: 11px; font-weight: 700; background: var(--surface-2); color: var(--ink-soft); }
    .unset { color: var(--muted); }
  `,
})
export class PointsBadgeComponent {
  readonly points = input<number | null | undefined>(null);
}
