import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="empty">
      <div class="icon"><mat-icon>{{ icon() }}</mat-icon></div>
      <h3>{{ title() }}</h3>
      @if (message()) {
        <p>{{ message() }}</p>
      }
      <ng-content />
    </div>
  `,
  styles: `
    .empty { display: flex; flex-direction: column; align-items: center; text-align: center; padding: 48px 24px; gap: 8px; }
    .icon { width: 56px; height: 56px; border-radius: 16px; background: var(--accent-soft); display: flex;
      align-items: center; justify-content: center; color: var(--accent); margin-bottom: 8px; }
    h3 { margin: 0; font-size: 16px; }
    p { margin: 0 0 8px; color: var(--muted); max-width: 420px; }
  `,
})
export class EmptyStateComponent {
  readonly icon = input('inbox');
  readonly title = input.required<string>();
  readonly message = input<string>();
}
