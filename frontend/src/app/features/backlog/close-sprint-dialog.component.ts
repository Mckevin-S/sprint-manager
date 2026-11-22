import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { Sprint } from '../../core/models';
import { NotifyService, errorMessage } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';

@Component({
  selector: 'app-close-sprint-dialog',
  standalone: true,
  imports: [FormsModule, MatDialogModule, MatFormFieldModule, MatSelectModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Clôturer {{ data.sprint.name }}</h2>
    <mat-dialog-content>
      <div class="summary">
        <div><strong>{{ data.sprint.donePoints }}</strong><span>points terminés</span></div>
        <div><strong>{{ data.sprint.totalPoints - data.sprint.donePoints }}</strong><span>points restants</span></div>
        <div><strong>{{ data.sprint.committedPoints ?? 0 }}</strong><span>points engagés</span></div>
      </div>
      <p class="muted">Les points terminés comptent pour la vélocité. Les tickets non terminés repartent « À faire » vers :</p>
      <mat-form-field appearance="outline">
        <mat-label>Destination des tickets non terminés</mat-label>
        <mat-select [(ngModel)]="target">
          <mat-option [value]="null">Le backlog</mat-option>
          @for (s of planned(); track s.id) {
            <mat-option [value]="s.id">{{ s.name }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      @if (error()) {
        <p class="error" role="alert">{{ error() }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" (click)="submit()" [disabled]="loading()">Clôturer le sprint</button>
    </mat-dialog-actions>
  `,
  styles: `
    .summary { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
    .summary div { background: var(--surface-2); border-radius: 10px; padding: 12px; display: flex; flex-direction: column; }
    .summary strong { font-size: 22px; }
    .summary span { font-size: 12px; color: var(--muted); }
    .error { color: var(--danger); }
  `,
})
export class CloseSprintDialogComponent {
  private readonly api = inject(ApiService);
  private readonly context = inject(ProjectContext);
  private readonly notify = inject(NotifyService);
  private readonly dialogRef = inject(MatDialogRef<CloseSprintDialogComponent, boolean>);
  readonly data = inject<{ sprint: Sprint }>(MAT_DIALOG_DATA);

  readonly planned = signal<Sprint[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  target: number | null = null;

  constructor() {
    this.api.sprints(this.context.id()).subscribe((sprints) => {
      const planned = sprints.filter((s) => s.status === 'PLANNED');
      this.planned.set(planned);
      this.target = planned[0]?.id ?? null;
    });
  }

  submit(): void {
    this.loading.set(true);
    this.api.closeSprint(this.context.id(), this.data.sprint.id, this.target).subscribe({
      next: (report) => {
        this.notify.success(`${report.sprint.name} clôturé : ${report.carriedOver} ticket(s) reporté(s) vers ${report.carriedTo}.`);
        this.dialogRef.close(true);
      },
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
