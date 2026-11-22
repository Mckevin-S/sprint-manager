import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { IssueDetail, IssueType, PRIORITIES, PRIORITY_LABELS, Priority, STORY_POINTS, Sprint } from '../../core/models';
import { errorMessage } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { IssueTypeIconComponent } from '../../shared/issue-type-icon.component';
import { PriorityIconComponent } from '../../shared/priority-icon.component';

export interface CreateIssueData {
  sprintId?: number;
}

@Component({
  selector: 'app-create-issue-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatButtonModule,
    MatButtonToggleModule, IssueTypeIconComponent, PriorityIconComponent, AvatarComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Créer un ticket</h2>
    <form [formGroup]="form" (ngSubmit)="submit()">
      <mat-dialog-content>
        <mat-button-toggle-group formControlName="type" aria-label="Type de ticket" class="types">
          @for (t of types; track t.value) {
            <mat-button-toggle [value]="t.value" [disabled]="t.value === 'STORY' && !context.canWriteStories()">
              <app-issue-type-icon [type]="t.value" /> {{ t.label }}
            </mat-button-toggle>
          }
        </mat-button-toggle-group>
        @if (!context.canWriteStories()) {
          <p class="hint muted">Les user stories sont rédigées par le Product Owner.</p>
        }

        <mat-form-field appearance="outline">
          <mat-label>Titre</mat-label>
          <input matInput formControlName="title" maxlength="160" cdkFocusInitial data-testid="issue-title" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="4"
                    placeholder="En tant que…, je veux…, afin de… Critères d'acceptation…"></textarea>
        </mat-form-field>

        <div class="grid">
          <mat-form-field appearance="outline">
            <mat-label>Priorité</mat-label>
            <mat-select formControlName="priority">
              <mat-select-trigger><span class="opt"><app-priority-icon [priority]="form.controls.priority.value" /> {{ priorityLabels[form.controls.priority.value] }}</span></mat-select-trigger>
              @for (p of priorities; track p) {
                <mat-option [value]="p"><span class="opt"><app-priority-icon [priority]="p" /> {{ priorityLabels[p] }}</span></mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Points</mat-label>
            <mat-select formControlName="storyPoints">
              <mat-option [value]="null">Non estimé</mat-option>
              @for (p of points; track p) {
                <mat-option [value]="p">{{ p }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Assigné à</mat-label>
            <mat-select formControlName="assigneeId">
              <mat-option [value]="null">Personne</mat-option>
              @for (m of context.members(); track m.user.id) {
                <mat-option [value]="m.user.id"><span class="opt"><app-avatar [user]="m.user" [size]="20" /> {{ m.user.fullName }}</span></mat-option>
              }
            </mat-select>
          </mat-form-field>
          @if (context.canPlan()) {
            <mat-form-field appearance="outline">
              <mat-label>Sprint</mat-label>
              <mat-select formControlName="sprintId">
                <mat-option [value]="null">Backlog</mat-option>
                @for (s of sprints(); track s.id) {
                  <mat-option [value]="s.id">{{ s.name }}{{ s.status === 'ACTIVE' ? ' (en cours)' : '' }}</mat-option>
                }
              </mat-select>
            </mat-form-field>
          }
        </div>
        @if (error()) {
          <p class="error" role="alert">{{ error() }}</p>
        }
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close>Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading()" data-testid="issue-submit">Créer</button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .types { margin-bottom: 4px; }
    .types app-issue-type-icon { margin-right: 6px; vertical-align: middle; }
    .hint { font-size: 12px; margin: 4px 0 0; }
    mat-form-field { margin-top: 12px; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; column-gap: 12px; }
    .grid mat-form-field { margin-top: 0; }
    .opt { display: inline-flex; align-items: center; gap: 8px; }
    .error { color: var(--danger); }
  `,
})
export class CreateIssueDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<CreateIssueDialogComponent, IssueDetail>);
  private readonly data = inject<CreateIssueData | null>(MAT_DIALOG_DATA, { optional: true });
  readonly context = inject(ProjectContext);

  readonly types: { value: IssueType; label: string }[] = [
    { value: 'STORY', label: 'Story' },
    { value: 'BUG', label: 'Bug' },
    { value: 'TASK', label: 'Tâche' },
  ];
  readonly priorities = PRIORITIES;
  readonly priorityLabels = PRIORITY_LABELS;
  readonly points = STORY_POINTS;
  readonly sprints = signal<Sprint[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    type: this.fb.nonNullable.control<IssueType>(this.context.canWriteStories() ? 'STORY' : 'TASK'),
    title: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(160)]),
    description: this.fb.control<string | null>(null),
    priority: this.fb.nonNullable.control<Priority>('MEDIUM'),
    storyPoints: this.fb.control<number | null>(null),
    assigneeId: this.fb.control<number | null>(null),
    sprintId: this.fb.control<number | null>(this.data?.sprintId ?? null),
  });

  constructor() {
    if (this.context.canPlan()) {
      this.api.sprints(this.context.id()).subscribe((sprints) => this.sprints.set(sprints.filter((s) => s.status !== 'CLOSED')));
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.api.createIssue(this.context.id(), this.form.getRawValue()).subscribe({
      next: (created) => this.dialogRef.close(created),
      error: (e) => {
        this.error.set(errorMessage(e));
        this.loading.set(false);
      },
    });
  }
}
