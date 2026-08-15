import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { TicketService } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import {
  PRIORITY_CLASS,
  PRIORITY_LABEL,
  Priority,
  STATUS_CLASS,
  STATUS_LABEL,
  Status,
  TicketResponse,
} from '../../../../models/ticket.model';
import { Role } from '../../../../models/user.model';
import { formatElapsed, parseApiDate } from '../../../../shared/util/time';
import { TicketForm } from '../../../../shared/components/ticket-form/ticket-form';
import { LifecycleRail } from '../../../../shared/components/lifecycle-rail/lifecycle-rail';

interface TicketRow {
  ticket: TicketResponse;
  dwell: string;
  statusClass: string;
  statusLabel: string;
  priorityClass: string;
  priorityLabel: string;
  owner: string | null;
}

const PAGE_SIZE = 20;

@Component({
  selector: 'app-ticket-list',
  imports: [FormsModule, RouterLink, TicketForm, LifecycleRail],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.css',
})
export class TicketList implements OnInit {
  constructor(
    private ticketService: TicketService,
    private userService: UserService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  rows: TicketRow[] = [];
  isLoading = true;
  loadError: string | null = null;

  keyword = '';
  selectedStatus: Status | '' = '';
  selectedPriority: Priority | '' = '';

  page = 0;
  totalPages = 0;
  totalElements = 0;
  readonly pageSize = PAGE_SIZE;

  showTicketForm = false;
  isStaff = false;

  statuses = Object.values(Status);
  priorities = Object.values(Priority);
  readonly statusLabel = STATUS_LABEL;
  readonly priorityLabel = PRIORITY_LABEL;

  /* TicketResponseDto returns assignedTo as a bare UUID, so a name needs a
     second lookup. GET /api/users is ADMIN-only, so admins get real names and
     everyone else gets assigned/unassigned — which is the actionable part
     anyway. Resolving names for all roles needs the DTO to carry them. */
  private userNames = new Map<string, string>();

  ngOnInit(): void {
    this.isStaff = this.authService.isStaff();

    if (this.authService.getRole() === Role.ADMIN) {
      this.userService.getUsers().subscribe({
        next: (users) => {
          for (const user of users ?? []) {
            if (user?.userId && user.profile) {
              this.userNames.set(
                user.userId,
                `${user.profile.firstName} ${user.profile.lastName}`.trim(),
              );
            }
          }
          this.load();
        },
        error: () => this.load(),
      });
    } else {
      this.load();
    }
  }

  /**
   * One code path for browsing and filtering. The advanced-search endpoint
   * handles an empty keyword ( LIKE '%%' matches every row ) and ignores null
   * filters, so there is no separate "unfiltered" fetch to keep in step — which
   * is what let the old filters clobber each other.
   */
  load(): void {
    this.isLoading = true;
    this.loadError = null;

    this.ticketService
      .advancedSearch(
        this.keyword.trim(),
        this.selectedStatus || null,
        this.selectedPriority || null,
        this.page,
        this.pageSize,
      )
      .subscribe({
        next: (response) => {
          this.rows = (response.content ?? []).map((ticket) => this.toRow(ticket));
          this.totalPages = response.page?.totalPages ?? 0;
          this.totalElements = response.page?.totalElements ?? 0;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error('Failed to load tickets', error);
          this.loadError = 'Could not load tickets. Check that the service desk is running.';
          this.isLoading = false;
          this.cdr.detectChanges();
        },
      });
  }

  private toRow(ticket: TicketResponse): TicketRow {
    return {
      ticket,
      dwell: formatElapsed(parseApiDate(ticket.updatedAt)),
      statusClass: STATUS_CLASS[ticket.status],
      statusLabel: STATUS_LABEL[ticket.status],
      priorityClass: PRIORITY_CLASS[ticket.priority],
      priorityLabel: PRIORITY_LABEL[ticket.priority],
      owner: ticket.assignedTo ? (this.userNames.get(ticket.assignedTo) ?? 'Assigned') : null,
    };
  }

  /** Any filter change restarts at the first page — page 4 of the old result
      set is meaningless against the new one. */
  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.keyword = '';
    this.selectedStatus = '';
    this.selectedPriority = '';
    this.applyFilters();
  }

  get hasFilters(): boolean {
    return !!(this.keyword.trim() || this.selectedStatus || this.selectedPriority);
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages || page === this.page) return;
    this.page = page;
    this.load();
  }

  get rangeStart(): number {
    return this.totalElements === 0 ? 0 : this.page * this.pageSize + 1;
  }

  get rangeEnd(): number {
    return Math.min((this.page + 1) * this.pageSize, this.totalElements);
  }

  viewTicket(id: number): void {
    this.router.navigate(['/tickets', id]);
  }

  deleteTicket(id: number, event: Event): void {
    event.stopPropagation();
    this.ticketService.deleteTicket(id).subscribe({
      next: () => this.load(),
      error: (error) => {
        console.error('Failed to delete ticket', error);
        this.loadError = 'Could not delete that ticket.';
        this.cdr.detectChanges();
      },
    });
  }

  onTicketCreated(): void {
    this.showTicketForm = false;
    this.page = 0;
    this.load();
  }
}
