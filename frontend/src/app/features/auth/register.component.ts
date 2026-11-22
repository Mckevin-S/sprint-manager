import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/notify.service';
import { AuthLayoutComponent } from './auth-layout.component';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, AuthLayoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-auth-layout>
      <h2>Créer un compte</h2>
      <p class="muted">Créez votre espace puis invitez votre équipe.</p>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Nom complet</mat-label>
          <input matInput formControlName="fullName" autocomplete="name" />
          @if (form.controls.fullName.invalid) {
            <mat-error>Entre 2 et 80 caractères.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>E-mail professionnel</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="email" />
          @if (form.controls.email.invalid) {
            <mat-error>Adresse e-mail invalide.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Mot de passe</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="new-password" />
          <mat-hint>8 caractères minimum</mat-hint>
          @if (form.controls.password.invalid) {
            <mat-error>8 caractères minimum.</mat-error>
          }
        </mat-form-field>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        <button mat-flat-button color="primary" class="submit" type="submit" [disabled]="loading()">
          {{ loading() ? 'Création…' : 'Créer mon compte' }}
        </button>
      </form>
      <p class="switch">Déjà inscrit ? <a routerLink="/connexion">Se connecter</a></p>
    </app-auth-layout>
  `,
  styles: `
    h2 { font-size: 26px; margin: 0 0 4px; }
    form { margin-top: 24px; display: flex; flex-direction: column; gap: 4px; }
    .submit { height: 44px; margin-top: 12px; }
    .error { color: var(--danger); margin: 0; }
    .switch { margin-top: 20px; color: var(--muted); }
  `,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { fullName, email, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.auth.register(fullName, email, password).subscribe({
      next: () => this.router.navigate(['/projets']),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
