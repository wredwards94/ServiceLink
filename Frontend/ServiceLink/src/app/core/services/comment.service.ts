import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CommentRequest, CommentResponse } from '../../models/comment.model';
import { PageResponse } from '../../models/ticket.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CommentService {
  constructor(private http: HttpClient) {}

  private apiUrl = `${environment.apiUrl}/api/comments`;

  addComment(ticketId: number, comment: CommentRequest): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`${this.apiUrl}/ticket/${ticketId}`, comment);
  }

  /**
   * Sorted oldest-first by the endpoint. The list embedded in a ticket has no
   * @OrderBy behind it, so this is the only read with a guaranteed order.
   */
  getCommentsForTicket(
    ticketId: number,
    page: number,
    size: number,
  ): Observable<PageResponse<CommentResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<CommentResponse>>(`${this.apiUrl}/ticket/${ticketId}`, {
      params,
    });
  }

  searchComments(
    ticketId: number,
    keyword: string,
    page: number = 0,
    size: number = 20,
  ): Observable<PageResponse<CommentResponse>> {
    const params = new HttpParams()
      .set('keyword', keyword)
      .set('page', page)
      .set('size', size);
    return this.http.get<PageResponse<CommentResponse>>(
      `${this.apiUrl}/ticket/${ticketId}/search`,
      { params },
    );
  }

  deleteComment(commentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${commentId}`);
  }

  /** PUT, not PATCH — SecurityConfig only admits a USER on the PUT rule. */
  updateComment(commentId: number, comment: CommentRequest): Observable<CommentResponse> {
    return this.http.put<CommentResponse>(`${this.apiUrl}/${commentId}`, comment);
  }
}
