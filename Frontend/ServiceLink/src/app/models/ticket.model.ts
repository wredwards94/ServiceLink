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
  status: Status;
  priority: Priority;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export enum Status {
  NEW = 'NEW',
  IN_PROGRESS = 'IN_PROGRESS',
  CLOSED = 'CLOSED',
}

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}
