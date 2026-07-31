import { Injectable } from '@angular/core';
import { CommentRequest, CommentResponse } from '../../models/comment.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';
import { PageResponse } from '../../models/ticket.model';

@Injectable({
  providedIn: 'root',
})
export class CommentService {
  constructor(private http: HttpClient) {}

  private apiUrl = `${environment.apiUrl}/api/comments`;

  addComment(
    ticketId: number,
    comment: CommentRequest,
  ): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`${this.apiUrl}/ticket/${ticketId}`, comment);
  }

  getCommentsForTicket(ticketId: number, page: number, size: number): Observable<PageResponse<CommentResponse>> {
    return this.http.get<PageResponse<CommentResponse>>(
      `${this.apiUrl}/ticket/${ticketId}?page=${page}&size=${size}`,
    );
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${commentId}`);
  }

  updateComment(commentId: number, comment: CommentRequest): Observable<CommentResponse> {
    return this.http.put<CommentResponse>(`${this.apiUrl}/${commentId}`, comment);
  }
}
