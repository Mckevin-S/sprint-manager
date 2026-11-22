import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../core/notify.service';
import { AuthLayoutComponent } from './auth-layout.component';

interface DemoAccount {
  email: string;
  name: string;
  role: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    AuthLayoutComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-auth-layout>
      <h2>Connexion</h2>
      <p class="muted">Content de vous revoir. Connectez-vous pour retrouver vos projets.</p>

      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>E-mail</mat-label>
          <input matInput type="email" formControlName="email" autocomplete="email" data-testid="email" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Mot de passe</mat-label>
          <input matInput [type]="showPassword() ? 'text' : 'password'" formControlName="password"
                 autocomplete="current-password" data-testid="password" />
          <button mat-icon-button matSuffix type="button" (click)="showPassword.set(!showPassword())"
                  [attr.aria-label]="showPassword() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'">
            <mat-icon>{{ showPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        </mat-form-field>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
        <button mat-flat-button color="primary" class="submit" type="submit" [disabled]="loading()" data-testid="login">
          {{ loading() ? 'Connexion…' : 'Se connecter' }}
        </button>
      </form>

      <p class="switch">Pas encore de compte ? <a routerLink="/inscription">Créer un compte</a></p>

      <section class="demo" aria-labelledby="demo-title">
        <h3 id="demo-title"><mat-icon>science</mat-icon> Explorer la démo</h3>
        <p class="muted">Un projet complet est préchargé. Choisissez un rôle pour voir l'application avec ses droits.</p>
        <div class="accounts">
          @for (account of demoAccounts; track account.email) {
            <button type="button" class="account" (click)="useDemo(account)" [attr.data-testid]="'demo-' + account.email">
              <strong>{{ account.role }}</strong>
              <span>{{ account.name }}</span>
            </button>
          }
        </div>
      </section>
    </app-auth-layout>
  `,
  styles: `
    h2 { font-size: 26px; margin: 0 0 4px; }
    form { margin-top: 24px; display: flex; flex-direction: column; gap: 4px; }
    .submit { height: 44px; margin-top: 4px; }
    .error { color: var(--danger); margin: 0 0 8px; }
    .switch { margin-top: 20px; color: var(--muted); }
    .demo { margin-top: 28px; padding: 16px; border: 1px dashed var(--border-strong); border-radius: var(--radius); }
    .demo h3 { margin: 0 0 4px; font-size: 14px; display: flex; align-items: center; gap: 6px; }
    .demo h3 mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--accent); }
    .demo p { margin: 0 0 12px; font-size: 13px; }
    .accounts { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
    .account { display: flex; flex-direction: column; align-items: flex-start; gap: 2px; padding: 10px; border-radius: 10px;
      border: 1px solid var(--border); background: var(--surface); cursor: pointer; font: inherit; color: var(--ink);
      text-align: left; transition: border-color 0.15s, transform 0.15s; }
    .account:hover { border-color: var(--accent); transform: translateY(-1px); }
    .account strong { font-size: 12px; }
    .account span { font-size: 12px; color: var(--muted); }
  `,
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly showPassword = signal(false);
  readonly demoAccounts: DemoAccount[] = [
    { email: 'camille@demo.fr', name: 'Camille Durand', role: 'Product Owner' },
    { email: 'hugo@demo.fr', name: 'Hugo Lefèvre', role: 'Scrum Master' },
    { email: 'mckevin@demo.fr', name: 'Mckevin Kamguem', role: 'Développeur' },
  ];

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  useDemo(account: DemoAccount): void {
    this.form.setValue({ email: account.email, password: 'demo1234' });
    this.submit();
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(email, password).subscribe({
      next: () => this.router.navigate(['/projets']),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
