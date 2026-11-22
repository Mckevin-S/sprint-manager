import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { User } from '../core/models';

const PALETTE = ['#E8590C', '#2F6FEB', '#1F9D63', '#8B5CF6', '#D6336C', '#0CA5B0', '#B7791F'];

/** Pastille aux initiales, couleur stable par utilisateur. */
@Component({
  selector: 'app-avatar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (user(); as u) {
      <span class="avatar" [style.background]="color()" [style.width.px]="size()" [style.height.px]="size()"
            [style.font-size.px]="size() * 0.4" [attr.title]="u.fullName" [attr.aria-label]="u.fullName">
        {{ u.initials }}
      </span>
    } @else {
      <span class="avatar empty" [style.width.px]="size()" [style.height.px]="size()" title="Non assigné"
            aria-label="Non assigné">?</span>
    }
  `,
  styles: `
    .avatar { display: inline-flex; align-items: center; justify-content: center; border-radius: 50%;
      color: #fff; font-weight: 600; flex-shrink: 0; letter-spacing: 0.02em; user-select: none; }
    .empty { background: transparent; border: 1.5px dashed var(--border-strong); color: var(--muted); font-size: 11px; }
  `,
})
export class AvatarComponent {
  readonly user = input<User | null | undefined>(null);
  readonly size = input(28);
  readonly color = computed(() => {
    const id = this.user()?.id ?? 0;
    return PALETTE[id % PALETTE.length];
  });
}
