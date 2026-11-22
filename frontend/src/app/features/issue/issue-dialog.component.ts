import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService, IssueChanges } from '../../core/api.service';
import {
  IssueDetail,
  IssueStatus,
  PRIORITIES,
  PRIORITY_LABELS,
  Priority,
  STATUS_LABELS,
  STORY_POINTS,
  TYPE_LABELS,
} from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { ProjectContext } from '../../core/project-context.service';
import { AvatarComponent } from '../../shared/avatar.component';
import { IssueTypeIconComponent } from '../../shared/issue-type-icon.component';
import { PriorityIconComponent } from '../../shared/priority-icon.component';
import { RelativeTimePipe } from '../../shared/relative-time.pipe';
import { StatusChipComponent } from '../../shared/status-chip.component';

export interface IssueDialogData {
  issueId: number;
}

/** Détail d'un ticket : édition, workflow, commentaires et historique. Renvoie true si le ticket a changé. */
@Component({
  selector: 'app-issue-dialog',
  standalone: true,
  imports: [FormsModule, DatePipe, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatSelectModule, MatTabsModule, MatTooltipModule, MatProgressBarModule, AvatarComponent, IssueTypeIconComponent,
    PriorityIconComponent, StatusChipComponent, RelativeTimePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './issue-dialog.component.html',
  styleUrl: './issue-dialog.component.scss',
})
export class IssueDialogComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly notify = inject(NotifyService);
  private readonly dialogRef = inject(MatDialogRef<IssueDialogComponent, boolean>);
  private readonly data = inject<IssueDialogData>(MAT_DIALOG_DATA);
  readonly context = inject(ProjectContext);

  readonly detail = signal<IssueDetail | null>(null);
  readonly saving = signal(false);
  readonly editingDescription = signal(false);
  readonly priorities = PRIORITIES;
  readonly priorityLabels = PRIORITY_LABELS;
  readonly statusLabels = STATUS_LABELS;
  readonly typeLabels = TYPE_LABELS;
  readonly points = STORY_POINTS;
  title = '';
  description = '';
  newComment = '';
  private changed = false;

  /** Les stories ne sont modifiables (contenu, priorité) que par le Product Owner. */
  readonly canEditContent = computed(() => {
    const d = this.detail();
    return !!d && (d.issue.type !== 'STORY' || this.context.isProductOwner());
  });

  ngOnInit(): void {
    this.dialogRef.backdropClick().subscribe(() => this.close());
    this.dialogRef.keydownEvents().subscribe((event) => {
      if (event.key === 'Escape') {
        this.close();
      }
    });
    this.reload();
  }

  reload(): void {
    this.api.issue(this.context.id(), this.data.issueId).subscribe({
      next: (detail) => this.apply(detail),
      error: (e) => {
        this.notify.error(e);
        this.dialogRef.close(this.changed);
      },
    });
  }

  move(status: IssueStatus): void {
    this.run(this.api.transition(this.context.id(), this.data.issueId, status));
  }

  saveTitle(): void {
    const d = this.detail();
    if (d && this.title.trim() && this.title.trim() !== d.issue.title) {
      this.update({ title: this.title.trim() });
    }
  }

  saveDescription(): void {
    this.editingDescription.set(false);
    if (this.description !== (this.detail()?.description ?? '')) {
      this.update({ description: this.description || null });
    }
  }

  update(changes: Partial<IssueChanges>): void {
    const d = this.detail();
    if (!d) {
      return;
    }
    const current: IssueChanges = {
      title: d.issue.title,
      description: d.description ?? null,
      priority: d.issue.priority,
      storyPoints: d.issue.storyPoints ?? null,
      assigneeId: d.issue.assignee?.id ?? null,
    };
    this.run(this.api.updateIssue(this.context.id(), this.data.issueId, { ...current, ...changes }));
  }

  setPriority(priority: Priority): void {
    this.update({ priority });
  }

  addComment(): void {
    const body = this.newComment.trim();
    if (!body) {
      return;
    }
    this.api.comment(this.context.id(), this.data.issueId, body).subscribe({
      next: () => {
        this.newComment = '';
        this.changed = true;
        this.reload();
      },
      error: (e) => this.notify.error(e),
    });
  }

  remove(): void {
    const d = this.detail();
    if (!d || !confirm(`Supprimer définitivement ${d.issue.key} ?`)) {
      return;
    }
    this.api.deleteIssue(this.context.id(), d.issue.id).subscribe({
      next: () => {
        this.notify.success(`${d.issue.key} supprimé.`);
        this.context.changed();
        this.dialogRef.close(true);
      },
      error: (e) => this.notify.error(e),
    });
  }

  close(): void {
    this.dialogRef.close(this.changed);
  }

  private run(request: ReturnType<ApiService['issue']>): void {
    this.saving.set(true);
    request.subscribe({
      next: (detail) => {
        this.changed = true;
        this.apply(detail);
        this.context.changed();
        this.saving.set(false);
      },
      error: (e) => {
        this.notify.error(e);
        this.saving.set(false);
        this.reload();
      },
    });
  }

  private apply(detail: IssueDetail): void {
    this.detail.set(detail);
    this.title = detail.issue.title;
    this.description = detail.description ?? '';
  }
}
