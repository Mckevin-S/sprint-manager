export type ProjectRole = 'PRODUCT_OWNER' | 'SCRUM_MASTER' | 'DEVELOPER';
export type IssueType = 'STORY' | 'BUG' | 'TASK';
export type IssueStatus = 'TODO' | 'IN_PROGRESS' | 'IN_REVIEW' | 'DONE';
export type Priority = 'LOWEST' | 'LOW' | 'MEDIUM' | 'HIGH' | 'HIGHEST';
export type SprintStatus = 'PLANNED' | 'ACTIVE' | 'CLOSED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  initials: string;
}

export interface AuthResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface Member {
  membershipId: number;
  user: User;
  role: ProjectRole;
  roleLabel: string;
  joinedAt: string;
}

export interface ProjectSummary {
  id: number;
  key: string;
  name: string;
  description?: string;
  myRole: ProjectRole;
  memberCount: number;
  activeSprint?: string;
  openIssues: number;
}

export interface ProjectDetail {
  id: number;
  key: string;
  name: string;
  description?: string;
  wipLimit: number;
  myRole: ProjectRole;
  members: Member[];
  createdAt: string;
}

export interface Issue {
  id: number;
  key: string;
  type: IssueType;
  title: string;
  status: IssueStatus;
  priority: Priority;
  storyPoints?: number;
  assignee?: User;
  sprintId?: number;
  rank: number;
  updatedAt: string;
  version: number;
}

export interface Comment {
  id: number;
  author: User;
  body: string;
  createdAt: string;
}

export interface Activity {
  id: number;
  type: string;
  issueKey?: string;
  summary: string;
  actor: User;
  occurredAt: string;
}

export interface IssueDetail {
  issue: Issue;
  description?: string;
  reporter: User;
  sprintName?: string;
  createdAt: string;
  resolvedAt?: string;
  nextStatuses: IssueStatus[];
  comments: Comment[];
  history: Activity[];
}

export interface Sprint {
  id: number;
  name: string;
  goal?: string;
  status: SprintStatus;
  startDate?: string;
  endDate?: string;
  committedPoints?: number;
  completedPoints?: number;
  issueCount: number;
  totalPoints: number;
  donePoints: number;
  daysRemaining?: number;
}

export interface BoardColumn {
  status: IssueStatus;
  label: string;
  issues: Issue[];
  points: number;
}

export interface Board {
  sprint?: Sprint;
  wipLimit: number;
  columns: BoardColumn[];
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface BurndownPoint {
  date: string;
  remaining?: number | null;
  ideal: number;
}

export interface Burndown {
  sprint?: Sprint;
  points: BurndownPoint[];
}

export interface VelocityEntry {
  sprintId: number;
  sprintName: string;
  committed: number;
  completed: number;
}

export interface Velocity {
  sprints: VelocityEntry[];
  averageCompleted: number;
  reliability?: number;
}

export interface Overview {
  activeSprint?: Sprint;
  completionPercent?: number;
  statuses: { status: IssueStatus; label: string; issues: number; points: number }[];
  workload: { member: User; issues: number; points: number; inProgress: number }[];
  openByType: { type: IssueType; label: string; issues: number }[];
  backlog: { issues: number; estimatedPoints: number; unestimatedStories: number };
  recentActivity: Activity[];
}

export interface Problem {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
}

export const STATUS_LABELS: Record<IssueStatus, string> = {
  TODO: 'À faire',
  IN_PROGRESS: 'En cours',
  IN_REVIEW: 'En revue',
  DONE: 'Terminé',
};

export const TYPE_LABELS: Record<IssueType, string> = { STORY: 'Story', BUG: 'Bug', TASK: 'Tâche' };

export const PRIORITY_LABELS: Record<Priority, string> = {
  LOWEST: 'Très basse',
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  HIGHEST: 'Critique',
};

export const ROLE_LABELS: Record<ProjectRole, string> = {
  PRODUCT_OWNER: 'Product Owner',
  SCRUM_MASTER: 'Scrum Master',
  DEVELOPER: 'Développeur',
};

export const STORY_POINTS = [1, 2, 3, 5, 8, 13, 21];
export const STATUSES: IssueStatus[] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'];
export const PRIORITIES: Priority[] = ['HIGHEST', 'HIGH', 'MEDIUM', 'LOW', 'LOWEST'];
