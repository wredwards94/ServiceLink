import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TicketService } from '../../../services/ticket.service';
import { UserService } from '../../../services/user.service';
import { ALLOWED_TRANSITIONS, Status, TicketResponse } from '../../../../models/ticket.model';
import { Profile, UserResponse } from '../../../../models/user.model';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CommentService } from '../../../services/comment.service';
import { AuthService } from '../../../services/auth.service';
import { CommentRequest } from '../../../../models/comment.model';

@Component({
  selector: 'app-ticket-detail',
  imports: [NgClass, FormsModule],
  templateUrl: './ticket-detail.html',
  styleUrl: './ticket-detail.css',
})
export class TicketDetail implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private ticketService: TicketService,
    private commentService: CommentService,
    private authService: AuthService,
    private userService: UserService,
    private cdr: ChangeDetectorRef,
  ) {}

  ticket: TicketResponse | null = null;
  isLoading: boolean = true;
  requester: UserResponse | null = null;
  assignedTo: UserResponse | null = null;
  requesterProfile: Profile | null = null;
  assignedToProfile: Profile | null = null;
  newComment: string = '';
  isSubmitting: boolean = false;
  isStaff: boolean = false;
  availableTransitions: Status[] = [];
  selectedStatus: Status | '' = '';
  statusError: string | null = null;
  isChangingStatus: boolean = false;

  ngOnInit(): void {
    this.isStaff = this.authService.isStaff();
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadTicket(Number(id));
    }
  }

  private applyTicket(ticket: TicketResponse): void {
    this.ticket = ticket;
    this.availableTransitions = ALLOWED_TRANSITIONS[ticket.status] ?? [];
  }

  loadTicket(id: number): void {
    this.ticketService.getTicketById(id).subscribe({
      next: (ticket) => {
        this.applyTicket(ticket);
        this.userService.getUserById(ticket.requester).subscribe({
          next: (requester) => {
            this.requester = requester;
            console.log('Requester Profile:', requester.profile);
            this.requesterProfile = requester.profile;
            this.cdr.detectChanges();
          },
          error: (error) => {
            console.error('Failed to load user profile', error);
            this.cdr.detectChanges();
          },
        });

        if (ticket.assignedTo) {
          this.userService.getUserById(ticket.assignedTo).subscribe({
            next: (assignedTo) => {
              this.assignedTo = assignedTo;
              this.assignedToProfile = assignedTo.profile;
              this.cdr.detectChanges();
            },
            error: (error) => {
              console.error('Failed to load user profile', error);
              this.cdr.detectChanges();
            },
          });
        } else {
          this.assignedToProfile = null;
          this.assignedTo = null;
        }
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load ticket', error);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  addComment(): void {
    if (!this.newComment.trim() || !this.ticket) return;

    const authorId = this.authService.getUserId();
    if (!authorId) return;

    const comment: CommentRequest = { content: this.newComment };
    this.isSubmitting = true;

    this.commentService.addComment(this.ticket.id, comment).subscribe({
      next: (comment) => {
        this.ticket!.comments.push(comment);
        this.newComment = '';
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to add comment', error);
        this.isSubmitting = false;
        this.cdr.detectChanges();
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  changeStatus(target: Status): void {
    if (!this.ticket || !target || this.isChangingStatus) return;

    this.isChangingStatus = true;
    this.statusError = null;

    this.ticketService.updateTicketStatus(this.ticket.id, target).subscribe({
      next: (updated) => {
        this.applyTicket({
          ...updated,
          comments: updated.comments ?? this.ticket?.comments ?? [],
        });
        this.selectedStatus = '';
        this.isChangingStatus = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to update status', err);
        this.statusError = err.error?.message ?? 'Failed to update status';
        this.selectedStatus = '';
        this.isChangingStatus = false;
        this.cdr.detectChanges();
      },
    });
  }
}
