import { CommentResponse } from './comment.model';

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  category: string;
  status: Status;
  priority: Priority;
  assignedTo: string;
  requester: string;
  createdAt: string;
  updatedAt: string;
  comments: CommentResponse[];
}

export interface TicketRequest {
  title: string;
  description: string;
  category: string;
  priority: Priority;
}

export interface PageResponse<T> {
  content: T[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  }
}

// Mirrors backend enums/TicketStatus.java — values must match exactly.
export enum Status {
  NEW = 'NEW',
  IN_PROGRESS = 'IN_PROGRESS',
  ON_HOLD = 'ON_HOLD',
  RESOLVED = 'RESOLVED',
  REOPENED = 'REOPENED',
  CLOSED = 'CLOSED',
}

// Mirrors backend enums/TicketPriority.java.
export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export const ALLOWED_TRANSITIONS: Record<Status, Status[]> = {
  [Status.NEW]: [Status.IN_PROGRESS],
  [Status.IN_PROGRESS]: [Status.ON_HOLD, Status.RESOLVED],
  [Status.ON_HOLD]: [Status.IN_PROGRESS],
  [Status.RESOLVED]: [Status.CLOSED, Status.REOPENED],
  [Status.REOPENED]: [Status.IN_PROGRESS],
  [Status.CLOSED]: [Status.REOPENED],
};
