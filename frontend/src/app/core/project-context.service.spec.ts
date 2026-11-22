import { TestBed } from '@angular/core/testing';
import { ProjectDetail, ProjectRole } from './models';
import { ProjectContext } from './project-context.service';

describe('ProjectContext', () => {
  let context: ProjectContext;

  const open = (role: ProjectRole) =>
    context.project.set({ id: 7, key: 'ORN', name: 'Orion', wipLimit: 3, myRole: role, members: [], createdAt: '' } as ProjectDetail);

  beforeEach(() => (context = TestBed.inject(ProjectContext)));

  it('donne au Product Owner tous les droits de pilotage', () => {
    open('PRODUCT_OWNER');
    expect(context.canWriteStories()).toBeTrue();
    expect(context.canPlan()).toBeTrue();
    expect(context.id()).toBe(7);
  });

  it('laisse le Scrum Master planifier sans rédiger de stories', () => {
    open('SCRUM_MASTER');
    expect(context.canPlan()).toBeTrue();
    expect(context.canWriteStories()).toBeFalse();
  });

  it('limite le développeur à ses tickets', () => {
    open('DEVELOPER');
    expect(context.canPlan()).toBeFalse();
    expect(context.canManageTeam()).toBeFalse();
  });

  it('signale les modifications aux vues ouvertes', () => {
    const before = context.revision();
    context.changed();
    expect(context.revision()).toBe(before + 1);
  });
});
