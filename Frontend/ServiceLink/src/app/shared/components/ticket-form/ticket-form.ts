import { Component, EventEmitter, HostListener, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../core/services/ticket.service';
import { PRIORITY_LABEL, Priority, TicketRequest } from '../../../models/ticket.model';

@Component({
  selector: 'app-ticket-form',
  imports: [FormsModule],
  templateUrl: './ticket-form.html',
  styleUrl: './ticket-form.css',
})
export class TicketForm {
  constructor(private ticketService: TicketService) {}

  @Output() closed = new EventEmitter<void>();
  @Output() ticketCreated = new EventEmitter<void>();

  isSubmitting = false;
  error: string | null = null;

  priorities = Object.values(Priority);
  readonly priorityLabel = PRIORITY_LABEL;

  /* New tickets are always NEW — TicketRequestDto has no status field, and the
     entity defaults it. There is deliberately no status control here. */
  ticket: TicketRequest = {
    title: '',
    description: '',
    priority: Priority.MEDIUM,
    category: '',
  };

  get isValid(): boolean {
    return !!(
      this.ticket.title.trim() &&
      this.ticket.description.trim() &&
      this.ticket.category.trim()
    );
  }

  @HostListener('document:keydown.escape')
  close(): void {
    if (this.isSubmitting) return;
    this.closed.emit();
  }

  submit(): void {
    if (!this.isValid || this.isSubmitting) return;

    this.isSubmitting = true;
    this.error = null;

    this.ticketService
      .createTicket({
        title: this.ticket.title.trim(),
        description: this.ticket.description.trim(),
        category: this.ticket.category.trim(),
        priority: this.ticket.priority,
      })
      .subscribe({
        next: () => {
          this.isSubmitting = false;
          this.ticketCreated.emit();
        },
        error: (err) => {
          console.error('Failed to create ticket', err);
          this.isSubmitting = false;
          this.error = err.error?.message ?? 'Could not file the ticket. Try again.';
        },
      });
  }
}
