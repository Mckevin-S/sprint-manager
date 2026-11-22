import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ChartConfiguration } from 'chart.js';
import { ApiService } from '../../core/api.service';
import { Burndown, Overview } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { ThemeService } from '../../core/theme.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { ChartComponent } from '../../shared/chart.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { IssueTypeIconComponent } from '../../shared/issue-type-icon.component';
import { RelativeTimePipe } from '../../shared/relative-time.pipe';
import { burndownChart } from '../reports/chart-configs';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, AvatarComponent, ChartComponent,
    EmptyStateComponent, IssueTypeIconComponent, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './overview.component.html',
  styleUrl: './overview.component.scss',
})
export class OverviewComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly theme = inject(ThemeService);
  readonly context = inject(ProjectContext);

  readonly overview = signal<Overview | null>(null);
  readonly burndown = signal<Burndown | null>(null);

  readonly maxLoad = computed(() => Math.max(1, ...(this.overview()?.workload ?? []).map((w) => w.points)));
  readonly totalIssues = computed(() => (this.overview()?.statuses ?? []).reduce((sum, s) => sum + s.issues, 0));
  readonly chart = computed<ChartConfiguration | null>(() => {
    this.theme.theme();
    const points = this.burndown()?.points ?? [];
    return points.length ? burndownChart(points) : null;
  });

  constructor() {
    effect(() => {
      this.context.revision();
      untracked(() => this.load());
    });
  }

  load(): void {
    const id = this.context.id();
    this.api.overview(id).subscribe({ next: (o) => this.overview.set(o), error: (e) => this.notify.error(e) });
    this.api.burndown(id).subscribe({ next: (b) => this.burndown.set(b), error: () => this.burndown.set(null) });
  }

  share(count: number): number {
    const total = this.totalIssues();
    return total ? (count * 100) / total : 0;
  }
}
