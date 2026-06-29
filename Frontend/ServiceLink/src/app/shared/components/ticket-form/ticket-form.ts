import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { Priority, Status, TicketRequest } from '../../../models/ticket.model';

@Component({
  selector: 'app-ticket-form',
  imports: [FormsModule],
  templateUrl: './ticket-form.html',
  styleUrl: './ticket-form.css',
})
export class TicketForm {
  constructor(
    private ticketService: TicketService,
    private authService: AuthService,
  ) {}

  @Output() closed = new EventEmitter<void>();
  @Output() ticketCreated = new EventEmitter<void>();

  isSubmitting: boolean = false;

  statuses = Object.values(Status);
  priorities = Object.values(Priority);

  ticket: TicketRequest = {
    title: '',
    description: '',
    status: Status.NEW,
    priority: Priority.LOW,
    category: '',
  };

  close(): void {
    this.closed.emit();
  }

  submit(): void {
    if (!this.ticket.title || !this.ticket.description || !this.ticket.category) return;

    const requesterId = this.authService.getUserId();
    if (!requesterId) return;

    this.isSubmitting = true;
    this.ticketService.createTicket(this.ticket, requesterId).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.ticketCreated.emit();
        this.close();
      },
      error: (error) => {
        console.error('Failed to create ticket', error);
        this.isSubmitting = false;
      },
    });
  }
}
