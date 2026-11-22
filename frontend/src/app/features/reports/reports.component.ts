import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChartConfiguration } from 'chart.js';
import { ApiService } from '../../core/api.service';
import { Burndown, Sprint, Velocity } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { ThemeService } from '../../core/theme.service';
import { ChartComponent } from '../../shared/chart.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { burndownChart, velocityChart } from './chart-configs';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [DatePipe, FormsModule, MatFormFieldModule, MatSelectModule, MatIconModule, MatTooltipModule, ChartComponent,
    EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Rapports</h1>
          <p>Indicateurs calculés à partir de l'historique réel des tickets.</p>
        </div>
      </div>

      <section class="kpis">
        <div class="card kpi">
          <span class="label">Vélocité moyenne</span>
          <span class="value">{{ velocity()?.averageCompleted ?? 0 }} <small>pts / sprint</small></span>
          <span class="hint">Sur les {{ velocity()?.sprints?.length ?? 0 }} derniers sprints clôturés</span>
        </div>
        <div class="card kpi">
          <span class="label">Fiabilité de l'engagement</span>
          <span class="value">{{ velocity()?.reliability ?? '–' }}<small>%</small></span>
          <span class="hint">Points terminés / points engagés</span>
        </div>
        <div class="card kpi">
          <span class="label">Prochain engagement conseillé</span>
          <span class="value">{{ suggested() }} <small>pts</small></span>
          <span class="hint" matTooltip="Vélocité moyenne arrondie à l'inférieur">Basé sur la vélocité moyenne</span>
        </div>
      </section>

      <div class="grid">
        <section class="card chart-card">
          <div class="chart-head">
            <div>
              <h2>Burndown</h2>
              @if (burndown()?.sprint; as s) {
                <p class="muted">{{ s.name }} · {{ s.startDate | date: 'd MMM' }} → {{ s.endDate | date: 'd MMM' }} ·
                  {{ s.committedPoints }} points engagés</p>
              }
            </div>
            <mat-form-field appearance="outline" class="select" subscriptSizing="dynamic">
              <mat-label>Sprint</mat-label>
              <mat-select [ngModel]="selectedSprint()" (ngModelChange)="selectSprint($event)">
                @for (s of startedSprints(); track s.id) {
                  <mat-option [value]="s.id">{{ s.name }}{{ s.status === 'ACTIVE' ? ' (en cours)' : '' }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          </div>
          @if (burndownConfig(); as config) {
            <app-chart [config]="config" [height]="320" label="Burndown du sprint" />
          } @else {
            <app-empty-state icon="trending_down" title="Pas encore de burndown" message="Démarrez un sprint pour suivre le travail restant jour après jour." />
          }
        </section>

        <section class="card chart-card">
          <div class="chart-head"><div><h2>Vélocité</h2><p class="muted">Points engagés et terminés par sprint</p></div></div>
          @if (velocityConfig(); as config) {
            <app-chart [config]="config" [height]="320" label="Vélocité de l'équipe" />
          } @else {
            <app-empty-state icon="bar_chart" title="Aucun sprint clôturé" message="La vélocité apparaîtra après la clôture du premier sprint." />
          }
        </section>
      </div>
    </div>
  `,
  styles: `
    .kpis { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 16px; }
    .kpi { padding: 18px 20px; display: flex; flex-direction: column; gap: 4px; }
    .kpi .label { font-size: 12px; color: var(--muted); font-weight: 600; text-transform: uppercase; letter-spacing: 0.04em; }
    .kpi .value { font-size: 30px; font-weight: 700; letter-spacing: -0.02em; }
    .kpi small { font-size: 14px; color: var(--muted); font-weight: 500; }
    .kpi .hint { font-size: 12px; color: var(--muted); }
    .grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
    .chart-card { padding: 20px; }
    .chart-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 12px; }
    .chart-head h2 { margin: 0; font-size: 17px; }
    .chart-head p { margin: 2px 0 0; font-size: 13px; }
    .select { width: 200px; }
    @media (max-width: 1100px) { .grid, .kpis { grid-template-columns: 1fr; } }
  `,
})
export class ReportsComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly theme = inject(ThemeService);
  private readonly context = inject(ProjectContext);

  readonly sprints = signal<Sprint[]>([]);
  readonly selectedSprint = signal<number | null>(null);
  readonly burndown = signal<Burndown | null>(null);
  readonly velocity = signal<Velocity | null>(null);

  readonly startedSprints = computed(() => this.sprints().filter((s) => s.status !== 'PLANNED'));
  readonly suggested = computed(() => Math.floor(Number(this.velocity()?.averageCompleted ?? 0)));

  readonly burndownConfig = computed<ChartConfiguration | null>(() => {
    this.theme.theme();
    const points = this.burndown()?.points ?? [];
    return points.length ? burndownChart(points) : null;
  });
  readonly velocityConfig = computed<ChartConfiguration | null>(() => {
    this.theme.theme();
    const v = this.velocity();
    return v && v.sprints.length ? velocityChart(v) : null;
  });

  constructor() {
    effect(() => {
      this.context.revision();
      untracked(() => this.load());
    });
  }

  load(): void {
    const id = this.context.id();
    this.api.velocity(id).subscribe({ next: (v) => this.velocity.set(v), error: (e) => this.notify.error(e) });
    this.api.sprints(id).subscribe({
      next: (sprints) => {
        this.sprints.set(sprints);
        const current = this.selectedSprint() ?? sprints.find((s) => s.status === 'ACTIVE')?.id
          ?? sprints.find((s) => s.status === 'CLOSED')?.id ?? null;
        this.selectSprint(current);
      },
      error: (e) => this.notify.error(e),
    });
  }

  selectSprint(sprintId: number | null): void {
    this.selectedSprint.set(sprintId);
    if (sprintId == null) {
      this.burndown.set(null);
      return;
    }
    this.api.burndown(this.context.id(), sprintId).subscribe({ next: (b) => this.burndown.set(b), error: (e) => this.notify.error(e) });
  }
}
