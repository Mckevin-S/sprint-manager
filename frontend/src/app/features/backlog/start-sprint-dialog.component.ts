import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { Sprint } from '../../core/models';
import { errorMessage } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';

/** Date locale au format ISO (AAAA-MM-JJ), sans décalage de fuseau. */
export function isoDate(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

@Component({
  selector: 'app-start-sprint-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatDatepickerModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Démarrer {{ data.sprint.name }}</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="muted">{{ data.sprint.issueCount }} ticket(s) · {{ data.sprint.totalPoints }} points seront engagés.
          L'engagement est figé au démarrage et sert de base au burndown.</p>
        <mat-form-field appearance="outline">
          <mat-label>Objectif du sprint</mat-label>
          <textarea matInput formControlName="goal" rows="2" maxlength="300" cdkFocusInitial
                    placeholder="Ce que l'équipe veut avoir livré à la fin du sprint"></textarea>
          @if (form.controls.goal.invalid) {
            <mat-error>L'objectif est obligatoire.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Période</mat-label>
          <mat-date-range-input [rangePicker]="picker">
            <input matStartDate formControlName="start" placeholder="Début" />
            <input matEndDate formControlName="end" placeholder="Fin" />
          </mat-date-range-input>
          <mat-datepicker-toggle matIconSuffix [for]="picker" />
          <mat-date-range-picker #picker />
          <mat-hint>4 semaines au plus. Par défaut : 2 semaines.</mat-hint>
        </mat-form-field>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading()" data-testid="start-submit">Démarrer le sprint</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `mat-form-field { margin-top: 8px; } .error { color: var(--danger); }`,
})
export class StartSprintDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly context = inject(ProjectContext);
  private readonly dialogRef = inject(MatDialogRef<StartSprintDialogComponent, boolean>);
  readonly data = inject<{ sprint: Sprint }>(MAT_DIALOG_DATA);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.group({
    goal: this.fb.nonNullable.control(this.data.sprint.goal ?? '', Validators.required),
    start: this.fb.control<Date | null>(new Date(), Validators.required),
    end: this.fb.control<Date | null>(new Date(Date.now() + 14 * 86_400_000), Validators.required),
  });

  submit(): void {
    const { goal, start, end } = this.form.getRawValue();
    if (this.form.invalid || !start || !end) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.api
      .startSprint(this.context.id(), this.data.sprint.id, { goal, startDate: isoDate(start), endDate: isoDate(end) })
      .subscribe({
        next: () => this.dialogRef.close(true),
        error: (e) => {
          this.error.set(errorMessage(e));
          this.loading.set(false);
        },
      });
  }
}
