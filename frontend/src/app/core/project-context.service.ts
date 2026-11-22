import { Injectable, computed, signal } from '@angular/core';
import { ProjectDetail, ProjectRole } from './models';

/**
 * Projet ouvert et droits de l'utilisateur courant. Les règles reprennent celles du back-end ;
 * elles servent à adapter l'interface, la vérification réelle reste côté serveur.
 */
@Injectable({ providedIn: 'root' })
export class ProjectContext {
  readonly project = signal<ProjectDetail | null>(null);
  /** Incrémenté après chaque modification, pour que les vues ouvertes se rechargent. */
  readonly revision = signal(0);

  readonly role = computed<ProjectRole | null>(() => this.project()?.myRole ?? null);
  readonly isProductOwner = computed(() => this.role() === 'PRODUCT_OWNER');
  readonly canPlan = computed(() => this.role() === 'PRODUCT_OWNER' || this.role() === 'SCRUM_MASTER');
  readonly canManageTeam = computed(() => this.canPlan());
  readonly canWriteStories = computed(() => this.isProductOwner());
  readonly members = computed(() => this.project()?.members ?? []);

  changed(): void {
    this.revision.update((r) => r + 1);
  }

  id(): number {
    const project = this.project();
    if (!project) {
      throw new Error('Aucun projet ouvert');
    }
    return project.id;
  }
}
