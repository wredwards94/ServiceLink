import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { CommentService } from '../../../services/comment.service';
import { AttachmentService } from '../../../services/attachment.service';
import { AuthService } from '../../../services/auth.service';
import {
  ALLOWED_TRANSITIONS,
  PRIORITY_CLASS,
  PRIORITY_LABEL,
  Priority,
  STATUS_CLASS,
  STATUS_LABEL,
  Status,
  TicketResponse,
} from '../../../../models/ticket.model';
import { COMMENT_MAX_LENGTH, CommentResponse } from '../../../../models/comment.model';
import { TicketHistoryEntry } from '../../../../models/history.model';
import { AttachmentResponse } from '../../../../models/attachment.model';
import { Profile, Role, UserResponse } from '../../../../models/user.model';
import { formatBytes, formatElapsed, formatStamp, parseApiDate } from '../../../../shared/util/time';
import { LifecycleRail } from '../../../../shared/components/lifecycle-rail/lifecycle-rail';

type Panel = 'activity' | 'history' | 'files';

interface AssignableUser {
  userId: string;
  name: string;
}

const COMMENT_PAGE_SIZE = 20;

@Component({
  selector: 'app-ticket-detail',
  imports: [FormsModule, RouterLink, LifecycleRail],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.css',
})
export class TicketDetail implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ticketService: TicketService,
    private commentService: CommentService,
    private attachmentService: AttachmentService,
    private authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  ticket: TicketResponse | null = null;
  isLoading = true;
  notFound = false;

  isStaff = false;
  isAdmin = false;
  currentUserId: string | null = null;

  requesterProfile: Profile | null = null;
  assignedToProfile: Profile | null = null;

  openedAt = '';
  dwell = '';

  // Status
  availableTransitions: Status[] = [];
  pendingStatus: Status | null = null;
  statusError: string | null = null;

  // Assignment
  assignableUsers: AssignableUser[] = [];
  selectedAssignee = '';
  isAssigning = false;
  assignError: string | null = null;

  // Panels
  panel: Panel = 'activity';

  // Comments
  comments: CommentResponse[] = [];
  commentPage = 0;
  commentTotalPages = 0;
  commentsLoading = false;
  newComment = '';
  newCommentInternal = false;
  isPostingComment = false;
  commentError: string | null = null;
  readonly commentMaxLength = COMMENT_MAX_LENGTH;

  // History
  history: TicketHistoryEntry[] = [];
  historyLoaded = false;
  historyLoading = false;
  historyError: string | null = null;

  // Attachments
  attachments: AttachmentResponse[] = [];
  attachmentsLoaded = false;
  attachmentsLoading = false;
  attachmentError: string | null = null;
  isUploading = false;

  readonly statusLabel = STATUS_LABEL;
  readonly formatStamp = formatStamp;
  readonly formatBytes = formatBytes;

  ngOnInit(): void {
    this.isStaff = this.authService.isStaff();
    this.isAdmin = this.authService.getRole() === Role.ADMIN;
    this.currentUserId = this.authService.getUserId();

    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.notFound = true;
      this.isLoading = false;
      return;
    }

    this.loadTicket(Number(id));

    if (this.isAdmin) {
      this.userService.getUsers().subscribe({
        next: (users) => {
          this.assignableUsers = (users ?? [])
            .filter((user): user is UserResponse => !!user?.userId && !!user.profile)
            .map((user) => ({
              userId: user.userId,
              name: `${user.profile.firstName} ${user.profile.lastName}`.trim(),
            }));
          this.cdr.detectChanges();
        },
        error: () => {},
      });
    }
  }

  // ---------------------------------------------------------------- ticket

  loadTicket(id: number): void {
    this.ticketService.getTicketById(id).subscribe({
      next: (ticket) => {
        this.applyTicket(ticket);
        this.loadComments(0);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load ticket', error);
        this.notFound = true;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private applyTicket(ticket: TicketResponse): void {
    this.ticket = ticket;
    this.availableTransitions = ALLOWED_TRANSITIONS[ticket.status] ?? [];
    this.openedAt = formatStamp(ticket.createdAt);
    this.dwell = formatElapsed(parseApiDate(ticket.updatedAt));

    this.userService.getUserById(ticket.requester).subscribe({
      next: (user) => {
        this.requesterProfile = user?.profile ?? null;
        this.cdr.detectChanges();
      },
      error: () => {},
    });

    // An unassigned ticket sends assignedTo: null. Looking that up unguarded
    // requests /api/users/null.
    if (ticket.assignedTo) {
      this.userService.getUserById(ticket.assignedTo).subscribe({
        next: (user) => {
          this.assignedToProfile = user?.profile ?? null;
          this.cdr.detectChanges();
        },
        error: () => {},
      });
    } else {
      this.assignedToProfile = null;
    }
  }

  get statusClass(): string {
    return this.ticket ? STATUS_CLASS[this.ticket.status] : '';
  }

  get priorityClass(): string {
    return this.ticket ? PRIORITY_CLASS[this.ticket.priority] : '';
  }

  get priorityText(): string {
    return this.ticket ? PRIORITY_LABEL[this.ticket.priority] : '';
  }

  get statusText(): string {
    return this.ticket ? STATUS_LABEL[this.ticket.status] : '';
  }

  get assigneeName(): string | null {
    if (!this.assignedToProfile) return null;
    return `${this.assignedToProfile.firstName} ${this.assignedToProfile.lastName}`.trim();
  }

  get requesterName(): string | null {
    if (!this.requesterProfile) return null;
    return `${this.requesterProfile.firstName} ${this.requesterProfile.lastName}`.trim();
  }

  // ---------------------------------------------------------------- status

  changeStatus(target: Status): void {
    if (!this.ticket || this.pendingStatus) return;

    this.pendingStatus = target;
    this.statusError = null;

    this.ticketService.updateTicketStatus(this.ticket.id, target).subscribe({
      next: (updated) => {
        this.applyTicket(updated);
        this.pendingStatus = null;
        // The move is an audited revision, so anything already fetched is stale.
        this.historyLoaded = false;
        if (this.panel === 'history') this.loadHistory();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to update status', err);
        // The rail cannot compose an illegal move, but the map mirroring
        // TicketStatus.canTransitionTo can drift — so the 400 still surfaces.
        this.statusError =
          err.error?.message ??
          (err.status === 403
            ? 'Only agents and admins can move a ticket.'
            : 'Could not change the status.');
        this.pendingStatus = null;
        this.cdr.detectChanges();
      },
    });
  }

  // ------------------------------------------------------------ assignment

  assignToMe(): void {
    if (!this.currentUserId) return;
    this.assign(this.currentUserId);
  }

  assignToSelected(): void {
    if (!this.selectedAssignee) return;
    this.assign(this.selectedAssignee);
  }

  private assign(userId: string): void {
    if (!this.ticket || this.isAssigning) return;

    this.isAssigning = true;
    this.assignError = null;

    this.ticketService.assignTicket(this.ticket.id, userId).subscribe({
      next: (updated) => {
        this.applyTicket(updated);
        this.selectedAssignee = '';
        this.isAssigning = false;
        this.historyLoaded = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to assign ticket', err);
        this.assignError = err.error?.message ?? 'Could not assign the ticket.';
        this.isAssigning = false;
        this.cdr.detectChanges();
      },
    });
  }

  unassign(): void {
    if (!this.ticket || this.isAssigning) return;

    this.isAssigning = true;
    this.assignError = null;

    this.ticketService.unassignTicket(this.ticket.id).subscribe({
      next: (updated) => {
        this.applyTicket(updated);
        this.isAssigning = false;
        this.historyLoaded = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to unassign ticket', err);
        this.assignError = err.error?.message ?? 'Could not unassign the ticket.';
        this.isAssigning = false;
        this.cdr.detectChanges();
      },
    });
  }

  // ---------------------------------------------------------------- panels

  showPanel(panel: Panel): void {
    this.panel = panel;
    if (panel === 'history' && !this.historyLoaded) this.loadHistory();
    if (panel === 'files' && !this.attachmentsLoaded) this.loadAttachments();
  }

  // -------------------------------------------------------------- comments

  /**
   * Comments come from GET /api/comments/ticket/{id}, not the list embedded in
   * the ticket. Only the endpoint sorts — Ticket.comments has no @OrderBy, so
   * its order is whatever Postgres happens to return.
   */
  loadComments(page: number): void {
    if (!this.ticket) return;

    this.commentsLoading = true;
    this.commentService.getCommentsForTicket(this.ticket.id, page, COMMENT_PAGE_SIZE).subscribe({
      next: (response) => {
        const batch = response.content ?? [];
        this.comments = page === 0 ? batch : [...this.comments, ...batch];
        this.commentPage = response.page?.number ?? page;
        this.commentTotalPages = response.page?.totalPages ?? 0;
        this.commentsLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load comments', error);
        this.commentsLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get hasMoreComments(): boolean {
    return this.commentPage + 1 < this.commentTotalPages;
  }

  loadMoreComments(): void {
    if (this.hasMoreComments && !this.commentsLoading) this.loadComments(this.commentPage + 1);
  }

  get commentRemaining(): number {
    return this.commentMaxLength - this.newComment.length;
  }

  get canPostComment(): boolean {
    return (
      !!this.newComment.trim() && this.commentRemaining >= 0 && !this.isPostingComment
    );
  }

  addComment(): void {
    if (!this.ticket || !this.canPostComment) return;

    this.isPostingComment = true;
    this.commentError = null;

    this.commentService
      .addComment(this.ticket.id, {
        content: this.newComment.trim(),
        internal: this.isStaff && this.newCommentInternal,
      })
      .subscribe({
        next: (comment) => {
          this.comments = [...this.comments, comment];
          this.newComment = '';
          this.newCommentInternal = false;
          this.isPostingComment = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Failed to add comment', err);
          this.commentError = err.error?.message ?? 'Could not post the comment.';
          this.isPostingComment = false;
          this.cdr.detectChanges();
        },
      });
  }

  // --------------------------------------------------------------- history

  loadHistory(): void {
    if (!this.ticket) return;

    this.historyLoading = true;
    this.historyError = null;

    this.ticketService.getTicketHistory(this.ticket.id).subscribe({
      next: (entries) => {
        // Newest first — the interesting end of an audit trail is the recent end.
        this.history = [...(entries ?? [])].sort((a, b) => b.revision - a.revision);
        this.historyLoaded = true;
        this.historyLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load history', error);
        this.historyError = 'Could not load the audit trail.';
        this.historyLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  /** Envers records raw enum values; show the same labels as everywhere else. */
  historyValue(value: string | null): string {
    if (!value) return '—';
    if (value in STATUS_LABEL) return STATUS_LABEL[value as Status];
    if (value in PRIORITY_LABEL) return PRIORITY_LABEL[value as Priority];
    return value;
  }

  /** Field names come through as entity properties. */
  historyField(field: string | null): string {
    if (!field) return '';
    return field.replace(/([A-Z])/g, ' $1').toLowerCase();
  }

  /** CREATED / DELETED rows carry no field detail, only the revision type. */
  historyType(type: string | null): string {
    if (!type) return 'changed';
    return type.toLowerCase();
  }

  // ----------------------------------------------------------- attachments

  loadAttachments(): void {
    if (!this.ticket) return;

    this.attachmentsLoading = true;
    this.attachmentError = null;

    this.attachmentService.listForTicket(this.ticket.id).subscribe({
      next: (files) => {
        this.attachments = files ?? [];
        this.attachmentsLoaded = true;
        this.attachmentsLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load attachments', error);
        this.attachmentError = 'Could not load the files on this ticket.';
        this.attachmentsLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || !this.ticket) return;

    this.isUploading = true;
    this.attachmentError = null;

    this.attachmentService.uploadToTicket(this.ticket.id, file).subscribe({
      next: (uploaded) => {
        this.attachments = [...this.attachments, uploaded];
        this.isUploading = false;
        input.value = '';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to upload attachment', err);
        this.attachmentError = err.error?.message ?? 'Could not attach that file.';
        this.isUploading = false;
        input.value = '';
        this.cdr.detectChanges();
      },
    });
  }

  download(file: AttachmentResponse): void {
    this.attachmentService.download(file.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = file.filename;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Failed to download attachment', error);
        this.attachmentError = 'Could not download that file.';
        this.cdr.detectChanges();
      },
    });
  }

  deleteAttachment(file: AttachmentResponse): void {
    this.attachmentService.deleteAttachment(file.id).subscribe({
      next: () => {
        this.attachments = this.attachments.filter((a) => a.id !== file.id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to delete attachment', err);
        this.attachmentError = err.error?.message ?? 'Could not remove that file.';
        this.cdr.detectChanges();
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/tickets']);
  }
}
