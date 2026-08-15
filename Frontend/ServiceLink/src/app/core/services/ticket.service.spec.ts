import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { TicketService } from './ticket.service';
import { Priority, Status } from '../../models/ticket.model';
import { environment } from '../../../environments/environment';

/**
 * These assert HTTP method and URL, because that is exactly the drift the
 * type system cannot catch — a wrong verb compiles perfectly and 405s at
 * runtime, which is how assignTicket and updateComment both broke.
 */
describe('TicketService', () => {
  let service: TicketService;
  let http: HttpTestingController;
  const base = `${environment.apiUrl}/api/tickets`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(TicketService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('assigns with PUT, not PATCH', () => {
    service.assignTicket(7, 'user-1').subscribe();

    const req = http.expectOne(`${base}/7/assign/user-1`);
    expect(req.request.method).toBe('PUT');
    req.flush({});
  });

  it('unassigns with PATCH', () => {
    service.unassignTicket(7).subscribe();

    const req = http.expectOne(`${base}/7/unassign`);
    expect(req.request.method).toBe('PATCH');
    req.flush({});
  });

  it('changes status with PATCH and a ticketStatus body key', () => {
    service.updateTicketStatus(7, Status.IN_PROGRESS).subscribe();

    const req = http.expectOne(`${base}/7/status`);
    expect(req.request.method).toBe('PATCH');
    // TicketStatusUpdateDto's record component is `ticketStatus`; any other
    // key fails @NotNull validation with a 400.
    expect(req.request.body).toEqual({ ticketStatus: 'IN_PROGRESS' });
    req.flush({});
  });

  it('creates a ticket without a requesterId param — identity is in the JWT', () => {
    service
      .createTicket({
        title: 'Mail relay down',
        description: 'Nothing is delivering',
        category: 'Network',
        priority: Priority.HIGH,
      })
      .subscribe();

    const req = http.expectOne((r) => r.url === `${base}/newticket/requester`);
    expect(req.request.method).toBe('POST');
    expect(req.request.params.has('requesterId')).toBe(false);
    expect(req.request.body).not.toHaveProperty('status');
    req.flush({});
  });

  it('omits null filters from advanced search entirely', () => {
    service.advancedSearch('relay', null, null, 0, 20).subscribe();

    const req = http.expectOne((r) => r.url === `${base}/search/advanced`);
    // An empty string does not convert to a TicketStatus — the param has to be
    // absent, not blank, or the request 400s.
    expect(req.request.params.has('status')).toBe(false);
    expect(req.request.params.has('priority')).toBe(false);
    expect(req.request.params.get('keyword')).toBe('relay');
    req.flush({ content: [], page: { size: 20, number: 0, totalElements: 0, totalPages: 0 } });
  });

  it('includes filters when they are set', () => {
    service.advancedSearch('', Status.ON_HOLD, Priority.CRITICAL, 1, 20).subscribe();

    const req = http.expectOne((r) => r.url === `${base}/search/advanced`);
    expect(req.request.params.get('status')).toBe('ON_HOLD');
    expect(req.request.params.get('priority')).toBe('CRITICAL');
    expect(req.request.params.get('page')).toBe('1');
    req.flush({ content: [], page: { size: 20, number: 1, totalElements: 0, totalPages: 0 } });
  });

  it('reads the audit trail', () => {
    service.getTicketHistory(7).subscribe((entries) => {
      expect(entries.length).toBe(1);
      expect(entries[0].revision).toBe(3);
    });

    const req = http.expectOne(`${base}/7/history`);
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        revision: 3,
        timestamp: '08/15/2026 09:30 PM',
        actorName: 'Rowan Moya',
        actorId: 'user-1',
        type: 'MODIFIED',
        field: 'status',
        oldValue: 'NEW',
        newValue: 'IN_PROGRESS',
      },
    ]);
  });
});
