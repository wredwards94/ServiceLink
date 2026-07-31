import { Injectable } from '@angular/core';
import { PageResponse, Status, TicketRequest, TicketResponse } from '../../models/ticket.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

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

  updateTicket(id: number, ticket: TicketRequest): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/${id}`, ticket);
  }

  updateTicketStatus(id: number, status: Status): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.apiUrl}/${id}/status`, { ticketStatus: status });
  }

  deleteTicket(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getTicketsByStatus(status: string): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/status/${status}`);
  }

  getTicketsByPriority(priority: string): Observable<TicketResponse[]> {
    return this.http.get<TicketResponse[]>(`${this.apiUrl}/priority/${priority}`);
  }

  searchTickets(
    keyword: string,
    page: number = 0,
    size: number = 10,
  ): Observable<PageResponse<TicketResponse>> {
    return this.http.get<PageResponse<TicketResponse>>(
      `${this.apiUrl}/search?keyword=${keyword}&page=${page}&size=${size}`,
    );
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
}
