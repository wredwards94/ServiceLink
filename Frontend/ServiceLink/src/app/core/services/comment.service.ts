import { Injectable } from '@angular/core';
import { CommentRequest, CommentResponse } from '../../models/comment.model';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CommentService {
  constructor(private http: HttpClient) {}

  private apiUrl = `${environment.apiUrl}/api/comments`;

  addComment(
    ticketId: number,
    authorId: string,
    comment: CommentRequest,
  ): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(
      `${this.apiUrl}/ticket/${ticketId}?authorId=${authorId}`,
      comment,
    );
  }

  getCommentsForTicket(ticketId: number): Observable<CommentResponse[]> {
    return this.http.get<CommentResponse[]>(`${this.apiUrl}/ticket/${ticketId}`);
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${commentId}`);
  }

  updateComment(commentId: number, comment: CommentRequest): Observable<CommentResponse> {
    return this.http.patch<CommentResponse>(`${this.apiUrl}/${commentId}`, comment);
  }
}
