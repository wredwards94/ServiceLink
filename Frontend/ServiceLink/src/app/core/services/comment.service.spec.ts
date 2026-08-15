import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { CommentService } from './comment.service';
import { environment } from '../../../environments/environment';

describe('CommentService', () => {
  let service: CommentService;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/api/comments`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CommentService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('edits with PUT, not PATCH', () => {
    service.updateComment(4, { content: 'revised', internal: false }).subscribe();

    const req = http.expectOne(`${base}/4`);
    // SecurityConfig only lets a USER through on PUT /api/comments/**; a PATCH
    // falls to the ADMIN/AGENT rule and 403s before it ever reaches a 405.
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('posts without an authorId param and carries the internal flag', () => {
    service.addComment(9, { content: 'looking now', internal: true }).subscribe();

    const req = http.expectOne((r) => r.url === `${base}/ticket/9`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.has('authorId')).toBe(false);
    expect(req.request.body).toEqual({ content: 'looking now', internal: true });
    req.flush({});
  });

  it('parses the nested page metadata of the VIA_DTO shape', () => {
    service.getCommentsForTicket(9, 0, 20).subscribe((response) => {
      expect(response.content.length).toBe(1);
      // Metadata is nested under $.page, not flattened onto the root.
      expect(response.page.totalElements).toBe(1);
      expect(response.page.totalPages).toBe(1);
    });

    const req = http.expectOne((r) => r.url === `${base}/ticket/9`);
    expect(req.request.method).toBe('GET');
    req.flush({
      content: [
        {
          id: 1,
          authorId: 'user-1',
          authorName: 'Rowan Moya',
          ticketId: 9,
          content: 'looking now',
          createdAt: '08/15/2026 09:30 PM',
          internal: false,
        },
      ],
      page: { size: 20, number: 0, totalElements: 1, totalPages: 1 },
    });
  });
});
