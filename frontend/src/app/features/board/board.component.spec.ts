import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { Board, Issue, ProjectDetail } from '../../core/models';
import { ProjectContext } from '../../core/project-context.service';
import { BoardComponent } from './board.component';

const ines = { id: 3, email: 'ines@demo.fr', fullName: 'Inès Moreau', initials: 'IM' };
const issue = (id: number, status: Issue['status'], assignee = ines): Issue => ({
  id, key: `ORN-${id}`, type: 'STORY', title: `Ticket ${id}`, status, priority: 'MEDIUM', storyPoints: 3,
  assignee, rank: id, updatedAt: '', version: 0,
});

function board(): Board {
  return {
    wipLimit: 1,
    sprint: { id: 4, name: 'Sprint 4', status: 'ACTIVE', issueCount: 3, totalPoints: 9, donePoints: 3, goal: 'Objectif' },
    columns: [
      { status: 'TODO', label: 'À faire', issues: [issue(1, 'TODO'), issue(2, 'TODO', { ...ines, id: 9 })], points: 6 },
      { status: 'IN_PROGRESS', label: 'En cours', issues: [issue(3, 'IN_PROGRESS')], points: 3 },
      { status: 'IN_REVIEW', label: 'En revue', issues: [], points: 0 },
      { status: 'DONE', label: 'Terminé', issues: [], points: 0 },
    ],
  };
}

describe('BoardComponent', () => {
  let http: HttpTestingController;

  function setup() {
    TestBed.configureTestingModule({
      imports: [BoardComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()],
    });
    TestBed.inject(ProjectContext).project.set({ id: 7, myRole: 'DEVELOPER', members: [] } as unknown as ProjectDetail);
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(BoardComponent);
    fixture.detectChanges();
    http.expectOne('/api/projects/7/board').flush(board());
    fixture.detectChanges();
    return fixture;
  }

  it('affiche les colonnes et la progression du sprint', () => {
    const fixture = setup();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Sprint 4');
    expect(fixture.componentInstance.progress()).toBe(33);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('app-issue-card').length).toBe(3);
  });

  it('filtre les cartes par membre et signale la limite WIP atteinte', () => {
    const fixture = setup();
    const component = fixture.componentInstance;
    const todo = component.board()!.columns[0];

    component.toggleAssignee(9);
    expect(component.visible(todo).map((i) => i.id)).toEqual([2]);
    expect(component.wipExceeded(component.board()!.columns[1])).toBeTrue();
  });

  it('recharge le tableau si le serveur refuse le déplacement', () => {
    const fixture = setup();
    const component = fixture.componentInstance;
    const [todo, doing] = component.board()!.columns;
    const moved = todo.issues[0];

    component.drop({
      previousContainer: { data: todo.issues },
      container: { data: doing.issues },
      item: { data: moved },
    } as unknown as CdkDragDrop<Issue[], Issue[], Issue>, 'IN_PROGRESS');

    expect(doing.issues.length).toBe(2);
    http.expectOne('/api/projects/7/issues/1/transition')
      .flush({ detail: 'Limite WIP atteinte', code: 'WIP_LIMIT_REACHED' }, { status: 422, statusText: 'Unprocessable' });
    http.expectOne('/api/projects/7/board').flush(board());
    expect(component.board()!.columns[1].issues.length).toBe(1);
  });
});
