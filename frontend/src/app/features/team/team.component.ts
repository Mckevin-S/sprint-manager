import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Member, ProjectRole, ROLE_LABELS } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { AvatarComponent } from '../../shared/avatar.component';

interface Permission {
  action: string;
  roles: ProjectRole[];
}

@Component({
  selector: 'app-team',
  standalone: true,
  imports: [DatePipe, FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatIconModule,
    MatTooltipModule, AvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Équipe</h1>
          <p>{{ context.members().length }} membre(s). Les droits suivent les rôles Scrum.</p>
        </div>
      </div>

      <div class="grid">
        <section class="card panel">
          @if (context.canManageTeam()) {
            <form class="invite" (ngSubmit)="invite()">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>E-mail du membre</mat-label>
                <input matInput type="email" name="email" [(ngModel)]="email" required placeholder="prenom.nom@entreprise.fr" />
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="role-field">
                <mat-label>Rôle</mat-label>
                <mat-select name="role" [(ngModel)]="role">
                  @for (r of roleKeys; track r) {
                    <mat-option [value]="r">{{ roles[r] }}</mat-option>
                  }
                </mat-select>
              </mat-form-field>
              <button mat-flat-button color="primary" type="submit" [disabled]="!email.trim() || busy()">Ajouter</button>
            </form>
            <p class="muted hint">La personne doit déjà avoir créé son compte.</p>
          }

          <ul class="members">
            @for (m of context.members(); track m.membershipId) {
              <li>
                <app-avatar [user]="m.user" [size]="38" />
                <div class="who">
                  <strong>{{ m.user.fullName }} @if (m.user.id === auth.user()?.id) { <span class="you">vous</span> }</strong>
                  <span class="muted">{{ m.user.email }} · depuis le {{ m.joinedAt | date: 'd MMM yyyy' }}</span>
                </div>
                @if (context.isProductOwner()) {
                  <mat-select class="role-select" [value]="m.role" (selectionChange)="changeRole(m, $event.value)"
                              [attr.aria-label]="'Rôle de ' + m.user.fullName">
                    @for (r of roleKeys; track r) {
                      <mat-option [value]="r">{{ roles[r] }}</mat-option>
                    }
                  </mat-select>
                  <button mat-icon-button (click)="remove(m)" matTooltip="Retirer du projet" [attr.aria-label]="'Retirer ' + m.user.fullName">
                    <mat-icon>person_remove</mat-icon>
                  </button>
                } @else {
                  <span [class]="'role-badge ' + m.role.toLowerCase()">{{ m.roleLabel }}</span>
                }
              </li>
            }
          </ul>
        </section>

        <section class="card panel">
          <h2>Qui peut faire quoi ?</h2>
          <table>
            <thead><tr><th>Action</th>@for (r of roleKeys; track r) {<th>{{ roles[r] }}</th>}</tr></thead>
            <tbody>
              @for (p of permissions; track p.action) {
                <tr>
                  <td>{{ p.action }}</td>
                  @for (r of roleKeys; track r) {
                    <td class="check">
                      @if (p.roles.includes(r)) {
                        <mat-icon class="yes" aria-label="Autorisé">check_circle</mat-icon>
                      } @else {
                        <mat-icon class="no" aria-label="Non autorisé">remove</mat-icon>
                      }
                    </td>
                  }
                </tr>
              }
            </tbody>
          </table>
          <p class="muted hint">Ces règles sont appliquées par l'API ; l'interface s'y adapte.</p>
        </section>
      </div>
    </div>
  `,
  styles: `
    .grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; align-items: start; }
    .panel { padding: 20px; }
    h2 { font-size: 16px; margin: 0 0 12px; }
    .invite { display: flex; gap: 8px; align-items: center; }
    .role-field { max-width: 180px; }
    .hint { font-size: 12px; margin: 6px 0 0; }
    .members { list-style: none; margin: 16px 0 0; padding: 0; }
    .members li { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-top: 1px solid var(--border); }
    .who { flex: 1; display: flex; flex-direction: column; }
    .who span { font-size: 12px; }
    .you { font-size: 11px; background: var(--accent-soft); color: var(--accent); padding: 1px 6px; border-radius: 6px; margin-left: 4px; font-weight: 600; }
    .role-select { width: 160px; padding: 6px 10px; border: 1px solid var(--border); border-radius: 8px; }
    .role-badge { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 8px; background: var(--surface-2); }
    .role-badge.product_owner { background: var(--accent-soft); color: var(--accent); }
    .role-badge.scrum_master { background: var(--status-review-bg); color: var(--status-review); }
    table { width: 100%; border-collapse: collapse; font-size: 13px; }
    th { text-align: left; font-size: 11px; text-transform: uppercase; letter-spacing: 0.04em; color: var(--muted); padding: 8px 6px; }
    td { padding: 8px 6px; border-top: 1px solid var(--border); }
    .check { text-align: center; }
    .yes { color: var(--success); font-size: 18px; width: 18px; height: 18px; }
    .no { color: var(--border-strong); font-size: 18px; width: 18px; height: 18px; }
    @media (max-width: 1100px) { .grid { grid-template-columns: 1fr; } }
  `,
})
export class TeamComponent {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  readonly context = inject(ProjectContext);
  readonly auth = inject(AuthService);

  readonly roles = ROLE_LABELS;
  readonly roleKeys: ProjectRole[] = ['PRODUCT_OWNER', 'SCRUM_MASTER', 'DEVELOPER'];
  readonly busy = signal(false);
  email = '';
  role: ProjectRole = 'DEVELOPER';

  readonly permissions: Permission[] = [
    { action: 'Rédiger et prioriser les stories', roles: ['PRODUCT_OWNER'] },
    { action: 'Créer bugs et tâches, estimer, commenter', roles: ['PRODUCT_OWNER', 'SCRUM_MASTER', 'DEVELOPER'] },
    { action: 'Planifier, démarrer et clôturer un sprint', roles: ['PRODUCT_OWNER', 'SCRUM_MASTER'] },
    { action: 'Faire avancer ses tickets', roles: ['PRODUCT_OWNER', 'SCRUM_MASTER', 'DEVELOPER'] },
    { action: "Faire avancer le ticket d'un autre", roles: ['PRODUCT_OWNER', 'SCRUM_MASTER'] },
    { action: 'Ajouter des membres', roles: ['PRODUCT_OWNER', 'SCRUM_MASTER'] },
    { action: 'Changer les rôles, retirer, supprimer', roles: ['PRODUCT_OWNER'] },
  ];

  invite(): void {
    this.busy.set(true);
    this.api.addMember(this.context.id(), this.email.trim(), this.role).subscribe({
      next: (member) => {
        this.notify.success(`${member.user.fullName} a rejoint le projet.`);
        this.email = '';
        this.busy.set(false);
        this.refresh();
      },
      error: (e) => {
        this.notify.error(e);
        this.busy.set(false);
      },
    });
  }

  changeRole(member: Member, role: ProjectRole): void {
    this.api.changeRole(this.context.id(), member.membershipId, role).subscribe({
      next: () => {
        this.notify.success(`${member.user.fullName} est maintenant ${this.roles[role]}.`);
        this.refresh();
      },
      error: (e) => {
        this.notify.error(e);
        this.refresh();
      },
    });
  }

  remove(member: Member): void {
    if (!confirm(`Retirer ${member.user.fullName} du projet ?`)) {
      return;
    }
    this.api.removeMember(this.context.id(), member.membershipId).subscribe({
      next: () => this.refresh(),
      error: (e) => this.notify.error(e),
    });
  }

  private refresh(): void {
    this.api.project(this.context.id()).subscribe((project) => {
      this.context.project.set(project);
      this.context.changed();
    });
  }
}
