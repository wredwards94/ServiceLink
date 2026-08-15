import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AttachmentResponse } from '../../models/attachment.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AttachmentService {
  constructor(private http: HttpClient) {}

  private apiUrl = `${environment.apiUrl}/api/attachments`;

  listForTicket(ticketId: number): Observable<AttachmentResponse[]> {
    return this.http.get<AttachmentResponse[]>(`${this.apiUrl}/ticket/${ticketId}`);
  }

  listForComment(commentId: number): Observable<AttachmentResponse[]> {
    return this.http.get<AttachmentResponse[]>(`${this.apiUrl}/comment/${commentId}`);
  }

  /**
   * The endpoint reads the file from a `file` request part. Content-Type is
   * deliberately not set — the browser has to supply the multipart boundary,
   * and setting the header by hand strips it.
   */
  uploadToTicket(ticketId: number, file: File): Observable<AttachmentResponse> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<AttachmentResponse>(`${this.apiUrl}/ticket/${ticketId}`, body);
  }

  uploadToComment(commentId: number, file: File): Observable<AttachmentResponse> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<AttachmentResponse>(`${this.apiUrl}/comment/${commentId}`, body);
  }

  /** Returns the raw bytes; the caller is responsible for handing them to the user. */
  download(attachmentId: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${attachmentId}/download`, {
      responseType: 'blob',
    });
  }

  deleteAttachment(attachmentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${attachmentId}`);
  }
}
