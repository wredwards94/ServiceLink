import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TicketService } from '../../services/ticket.service';
import {
  OPEN_STATUSES,
  PRIORITY_CLASS,
  PRIORITY_LABEL,
  Priority,
  STATUS_CLASS,
  STATUS_LABEL,
  Status,
  TicketResponse,
} from '../../../models/ticket.model';
import { formatElapsed, parseApiDate } from '../../../shared/util/time';
import { LifecycleRail } from '../../../shared/components/lifecycle-rail/lifecycle-rail';

/** One band of a distribution bar. */
interface Segment {
  label: string;
  count: number;
  pct: number;
  className: string;
}

/** A ticket prepared for display, so the template does no computation. */
interface QueueRow {
  ticket: TicketResponse;
  dwell: string;
  statusClass: string;
  statusLabel: string;
  priorityClass: string;
  priorityLabel: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [LifecycleRail, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  constructor(
    private ticketService: TicketService,
    public router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  isLoading = true;
  loadError: string | null = null;

  total = 0;
  openCount = 0;
  lifecycle: Segment[] = [];
  severity: Segment[] = [];
  quietest: QueueRow[] = [];

  /* Ordered dimmest-last: work that has just arrived is the brightest thing in
     the bar, work that is finished recedes. Status is luminance, never hue. */
  private readonly lifecycleOrder: Status[] = [
    Status.NEW,
    Status.IN_PROGRESS,
    Status.REOPENED,
    Status.ON_HOLD,
    Status.RESOLVED,
    Status.CLOSED,
  ];

  private readonly lifecycleClass: Record<Status, string> = {
    [Status.NEW]: 'seg-l0',
    [Status.IN_PROGRESS]: 'seg-l1',
    [Status.REOPENED]: 'seg-l2',
    [Status.ON_HOLD]: 'seg-hold',
    [Status.RESOLVED]: 'seg-l3',
    [Status.CLOSED]: 'seg-l4',
  };

  private readonly severityClass: Record<Priority, string> = {
    [Priority.LOW]: 'seg-low',
    [Priority.MEDIUM]: 'seg-medium',
    [Priority.HIGH]: 'seg-high',
    [Priority.CRITICAL]: 'seg-critical',
  };

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.isLoading = true;
    this.loadError = null;

    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        this.build(tickets ?? []);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load tickets', error);
        this.loadError = 'Could not load the queue. Check that the service desk is running.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private build(tickets: TicketResponse[]): void {
    this.total = tickets.length;
    this.openCount = tickets.filter((t) => OPEN_STATUSES.includes(t.status)).length;

    this.lifecycle = this.distribute(
      this.lifecycleOrder,
      tickets,
      (t) => t.status,
      (s) => STATUS_LABEL[s],
      (s) => this.lifecycleClass[s],
    );

    this.severity = this.distribute(
      [Priority.CRITICAL, Priority.HIGH, Priority.MEDIUM, Priority.LOW],
      tickets,
      (t) => t.priority,
      (p) => PRIORITY_LABEL[p],
      (p) => this.severityClass[p],
    );

    // Longest-quiet first: the open tickets nothing has happened to in the
    // longest time. This is dwell, not a deadline — the backend has no SLA
    // field and nothing here implies one.
    const now = new Date();
    this.quietest = tickets
      .filter((t) => OPEN_STATUSES.includes(t.status))
      .map((ticket) => ({ ticket, moved: parseApiDate(ticket.updatedAt) }))
      .sort((a, b) => (a.moved?.getTime() ?? 0) - (b.moved?.getTime() ?? 0))
      .slice(0, 8)
      .map(({ ticket, moved }) => ({
        ticket,
        dwell: formatElapsed(moved, now),
        statusClass: STATUS_CLASS[ticket.status],
        statusLabel: STATUS_LABEL[ticket.status],
        priorityClass: PRIORITY_CLASS[ticket.priority],
        priorityLabel: PRIORITY_LABEL[ticket.priority],
      }));
  }

  private distribute<K extends string>(
    keys: K[],
    tickets: TicketResponse[],
    pick: (t: TicketResponse) => K,
    label: (k: K) => string,
    className: (k: K) => string,
  ): Segment[] {
    const total = tickets.length;
    return keys
      .map((key) => {
        const count = tickets.filter((t) => pick(t) === key).length;
        return {
          label: label(key),
          count,
          pct: total ? (count / total) * 100 : 0,
          className: className(key),
        };
      })
      .filter((segment) => segment.count > 0);
  }

  viewTicket(ticketId: number): void {
    this.router.navigate(['/tickets', ticketId]);
  }
}
