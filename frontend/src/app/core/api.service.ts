import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Activity,
  Board,
  Burndown,
  Comment,
  Issue,
  IssueDetail,
  IssueStatus,
  IssueType,
  Member,
  Overview,
  Page,
  Priority,
  ProjectDetail,
  ProjectRole,
  ProjectSummary,
  Sprint,
  Velocity,
} from './models';

export interface IssueQuery {
  sprintId?: number;
  backlog?: boolean;
  status?: IssueStatus;
  type?: IssueType;
  assigneeId?: number;
  q?: string;
  page?: number;
  size?: number;
}

export interface IssueDraft {
  type: IssueType;
  title: string;
  description?: string | null;
  priority: Priority;
  storyPoints?: number | null;
  assigneeId?: number | null;
  sprintId?: number | null;
}

export interface IssueChanges {
  title: string;
  description?: string | null;
  priority: Priority;
  storyPoints?: number | null;
  assigneeId?: number | null;
}

/** Client typé de l'API REST. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Projets et équipe
  projects(): Observable<ProjectSummary[]> {
    return this.http.get<ProjectSummary[]>('/api/projects');
  }

  createProject(body: { key: string; name: string; description?: string }): Observable<ProjectDetail> {
    return this.http.post<ProjectDetail>('/api/projects', body);
  }

  project(id: number): Observable<ProjectDetail> {
    return this.http.get<ProjectDetail>(`/api/projects/${id}`);
  }

  updateProject(id: number, body: { name: string; description?: string; wipLimit: number }): Observable<ProjectDetail> {
    return this.http.put<ProjectDetail>(`/api/projects/${id}`, body);
  }

  addMember(projectId: number, email: string, role: ProjectRole): Observable<Member> {
    return this.http.post<Member>(`/api/projects/${projectId}/members`, { email, role });
  }

  changeRole(projectId: number, membershipId: number, role: ProjectRole): Observable<Member> {
    return this.http.put<Member>(`/api/projects/${projectId}/members/${membershipId}`, { role });
  }

  removeMember(projectId: number, membershipId: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectId}/members/${membershipId}`);
  }

  activity(projectId: number, page = 0, size = 30): Observable<Page<Activity>> {
    return this.http.get<Page<Activity>>(`/api/projects/${projectId}/activity`, { params: { page, size } });
  }

  // Tickets
  issues(projectId: number, query: IssueQuery = {}): Observable<Page<Issue>> {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    });
    return this.http.get<Page<Issue>>(`/api/projects/${projectId}/issues`, { params });
  }

  issue(projectId: number, issueId: number): Observable<IssueDetail> {
    return this.http.get<IssueDetail>(`/api/projects/${projectId}/issues/${issueId}`);
  }

  createIssue(projectId: number, draft: IssueDraft): Observable<IssueDetail> {
    return this.http.post<IssueDetail>(`/api/projects/${projectId}/issues`, draft);
  }

  updateIssue(projectId: number, issueId: number, changes: IssueChanges): Observable<IssueDetail> {
    return this.http.put<IssueDetail>(`/api/projects/${projectId}/issues/${issueId}`, changes);
  }

  transition(projectId: number, issueId: number, status: IssueStatus): Observable<IssueDetail> {
    return this.http.post<IssueDetail>(`/api/projects/${projectId}/issues/${issueId}/transition`, { status });
  }

  plan(projectId: number, issueId: number, sprintId: number | null): Observable<Issue> {
    return this.http.put<Issue>(`/api/projects/${projectId}/issues/${issueId}/sprint`, { sprintId });
  }

  rank(projectId: number, issueIds: number[]): Observable<void> {
    return this.http.put<void>(`/api/projects/${projectId}/issues/rank`, { issueIds });
  }

  deleteIssue(projectId: number, issueId: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectId}/issues/${issueId}`);
  }

  comment(projectId: number, issueId: number, body: string): Observable<Comment> {
    return this.http.post<Comment>(`/api/projects/${projectId}/issues/${issueId}/comments`, { body });
  }

  // Sprints
  sprints(projectId: number): Observable<Sprint[]> {
    return this.http.get<Sprint[]>(`/api/projects/${projectId}/sprints`);
  }

  createSprint(projectId: number, body: { name?: string; goal?: string }): Observable<Sprint> {
    return this.http.post<Sprint>(`/api/projects/${projectId}/sprints`, body);
  }

  startSprint(projectId: number, sprintId: number, body: { goal: string; startDate: string; endDate: string }): Observable<Sprint> {
    return this.http.post<Sprint>(`/api/projects/${projectId}/sprints/${sprintId}/start`, body);
  }

  closeSprint(
    projectId: number,
    sprintId: number,
    moveToSprintId: number | null,
  ): Observable<{ sprint: Sprint; carriedOver: number; carriedTo: string }> {
    return this.http.post<{ sprint: Sprint; carriedOver: number; carriedTo: string }>(
      `/api/projects/${projectId}/sprints/${sprintId}/close`,
      { moveToSprintId },
    );
  }

  deleteSprint(projectId: number, sprintId: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${projectId}/sprints/${sprintId}`);
  }

  board(projectId: number): Observable<Board> {
    return this.http.get<Board>(`/api/projects/${projectId}/board`);
  }

  // Rapports
  burndown(projectId: number, sprintId?: number): Observable<Burndown> {
    const options = sprintId ? { params: { sprintId } } : {};
    return this.http.get<Burndown>(`/api/projects/${projectId}/reports/burndown`, options);
  }

  velocity(projectId: number): Observable<Velocity> {
    return this.http.get<Velocity>(`/api/projects/${projectId}/reports/velocity`);
  }

  overview(projectId: number): Observable<Overview> {
    return this.http.get<Overview>(`/api/projects/${projectId}/reports/overview`);
  }
}
