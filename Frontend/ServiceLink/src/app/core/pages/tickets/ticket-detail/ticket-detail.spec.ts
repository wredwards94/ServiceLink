import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { TicketDetail } from './ticket-detail';
import { Priority, Status } from '../../../../models/ticket.model';
import { environment } from '../../../../../environments/environment';

const ticket = {
  id: 42,
  title: 'Mail relay down',
  description: 'Nothing is delivering',
  category: 'Network',
  status: Status.NEW,
  priority: Priority.HIGH,
  assignedTo: null,
  requester: 'user-1',
  createdAt: '08/15/2026 09:30 PM',
  updatedAt: '08/15/2026 09:30 PM',
  comments: [],
};

describe('TicketDetail', () => {
  let component: TicketDetail;
  let fixture: ComponentFixture<TicketDetail>;
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [TicketDetail],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '42' }) } },
        },
      ],
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TicketDetail);
    component = fixture.componentInstance;
  });

  afterEach(() => localStorage.clear());

  it('should create', () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/api/tickets/42`).flush(ticket);
    expect(component).toBeTruthy();
  });

  it('never looks up a profile for an unassigned ticket', () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/api/tickets/42`).flush(ticket);

    // The requester is always resolved; assignedTo is null here, and looking
    // it up unguarded would request /api/users/null.
    http.expectOne(`${environment.apiUrl}/api/users/user-1`).flush({});
    http.expectNone(`${environment.apiUrl}/api/users/null`);
    expect(component.assignedToProfile).toBeNull();
  });

  it('reads comments from the sorted endpoint, not the embedded list', () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/api/tickets/42`).flush(ticket);
    http.expectOne(`${environment.apiUrl}/api/users/user-1`).flush({});

    const req = http.expectOne((r) => r.url === `${environment.apiUrl}/api/comments/ticket/42`);
    expect(req.request.method).toBe('GET');
  });

  it('offers only the legal move out of NEW', () => {
    fixture.detectChanges();
    http.expectOne(`${environment.apiUrl}/api/tickets/42`).flush(ticket);

    expect(component.availableTransitions).toEqual([Status.IN_PROGRESS]);
  });
});
