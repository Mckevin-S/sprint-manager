import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiService } from '../../core/api.service';
import { ProjectDetail } from '../../core/models';
import { errorMessage } from '../../core/notify.service';

@Component({
  selector: 'app-create-project-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Nouveau projet</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <p class="muted intro">Vous en serez le Product Owner. Vous pourrez ensuite inviter l'équipe.</p>
        <div class="grid">
          <mat-form-field appearance="outline">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="name" (input)="suggestKey()" cdkFocusInitial />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Clé</mat-label>
            <input matInput formControlName="key" maxlength="6" class="key" />
            <mat-hint>Préfixe des tickets : {{ form.controls.key.value || 'ABC' }}-1</mat-hint>
            @if (form.controls.key.invalid) {
              <mat-error>2 à 6 lettres ou chiffres, en commençant par une lettre.</mat-error>
            }
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="3"></textarea>
        </mat-form-field>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading()">Créer le projet</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .intro { margin-top: 0; }
    .grid { display: grid; grid-template-columns: 2fr 1fr; gap: 12px; }
    .key { text-transform: uppercase; font-weight: 600; letter-spacing: 0.06em; }
    .error { color: var(--danger); }
  `,
})
export class CreateProjectDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<CreateProjectDialogComponent, ProjectDetail>);
  private keyEdited = false;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(80)]],
    key: ['', [Validators.required, Validators.pattern(/^[A-Za-z][A-Za-z0-9]{1,5}$/)]],
    description: ['', Validators.maxLength(500)],
  });

  constructor() {
    this.form.controls.key.valueChanges.subscribe(() => (this.keyEdited = this.form.controls.key.dirty));
  }

  /** Propose une clé à partir des initiales du nom, tant que l'utilisateur ne l'a pas modifiée. */
  suggestKey(): void {
    if (this.keyEdited) {
      return;
    }
    const words = this.form.controls.name.value
      .normalize('NFD')
      .replace(/[^A-Za-z0-9 ]/g, '')
      .split(/\s+/)
      .filter(Boolean);
    const key = words.length > 1 ? words.map((w) => w[0]).join('') : (words[0] ?? '').slice(0, 3);
    this.form.controls.key.setValue(key.toUpperCase().slice(0, 6), { emitEvent: false });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.loading.set(true);
    this.api.createProject({ ...value, key: value.key.toUpperCase() }).subscribe({
      next: (project) => this.dialogRef.close(project),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
