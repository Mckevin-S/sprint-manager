import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Logo « Cadence » : trois barres décroissantes, évocation du burndown. */
@Component({
  selector: 'app-logo',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="logo" [class.compact]="compact()">
      <svg viewBox="0 0 32 32" width="28" height="28" aria-hidden="true">
        <rect width="32" height="32" rx="9" class="tile" />
        <rect x="8" y="8" width="4" height="16" rx="2" class="bar" />
        <rect x="14" y="12" width="4" height="12" rx="2" class="bar" opacity="0.8" />
        <rect x="20" y="17" width="4" height="7" rx="2" class="bar" opacity="0.6" />
      </svg>
      @if (!compact()) {
        <span class="name">Cadence</span>
      }
    </span>
  `,
  styles: `
    .logo { display: inline-flex; align-items: center; gap: 10px; }
    .tile { fill: var(--accent); }
    .bar { fill: #fff; }
    .name { font-weight: 700; font-size: 18px; letter-spacing: -0.02em; color: inherit; }
  `,
})
export class LogoComponent {
  readonly compact = input(false);
}
