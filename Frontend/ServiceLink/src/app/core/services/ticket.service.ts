import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PageResponse,
  Priority,
  Status,
  TicketRequest,
  TicketResponse,
  TicketUpdate,
} from '../../models/ticket.model';
import { TicketHistoryEntry } from '../../models/history.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class TicketService {
  constructor(private http: HttpClient) {}

  private apiUrl = `${environment.apiUrl}/api/tickets`;

  getAllTickets(): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(this.apiUrl);
  }

  getTicketById(id: number): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.apiUrl}/${id}`);
  }

  createTicket(ticket: TicketRequest): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`${this.apiUrl}/newticket/requester`, ticket);
  }

  /** PATCH is a partial update — omitted fields are left unchanged server-side. */
  updateTicket(id: number, ticket: TicketUpdate): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/${id}`, ticket);
  }

  /** Body key is `ticketStatus`, matching TicketStatusUpdateDto — a mismatch 400s. */
  updateTicketStatus(id: number, status: Status): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/${id}/status`, {
      ticketStatus: status,
    });
  }

  deleteTicket(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getTicketsByStatus(status: Status): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/status/${status}`);
  }

  getTicketsByPriority(priority: Priority): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/priority/${priority}`);
  }

  searchTickets(
    keyword: string,
    page: number = 0,
    size: number = 10,
  ): Observable<PageResponse<TicketResponse>> {
    const params = new HttpParams()
      .set('keyword', keyword)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<TicketResponse>>(`${this.apiUrl}/search`, { params });
  }

  /**
   * Keyword plus optional status/priority narrowing. Omitted filters must not
   * appear in the query string at all — the backend binds them as nullable
   * enums, and an empty string fails conversion with a 400.
   */
  advancedSearch(
    keyword: string,
    status: Status | null,
    priority: Priority | null,
    page: number = 0,
    size: number = 10,
  ): Observable<PageResponse<TicketResponse>> {
    let params = new HttpParams()
      .set('keyword', keyword)
      .set('page', page)
      .set('size', size);
    if (status) params = params.set('status', status);
    if (priority) params = params.set('priority', priority);

    return this.http.get<PageResponse<TicketResponse>>(`${this.apiUrl}/search/advanced`, {
      params,
    });
  }

  getTicketsByRequester(requesterId: string): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/requester/${requesterId}`);
  }

  getTicketsAssignedToUser(userId: string): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/assigned/${userId}`);
  }

  assignTicket(ticketId: number, userId: string): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.apiUrl}/${ticketId}/assign/${userId}`, {});
  }

  unassignTicket(ticketId: number): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/${ticketId}/unassign`, {});
  }

  /** Envers audit trail, newest revision last. */
  getTicketHistory(id: number): Observable<TicketHistoryEntry[]> {
    return this.http.get<TicketHistoryEntry[]>(`${this.apiUrl}/${id}/history`);
  }
}
