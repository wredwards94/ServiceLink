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
