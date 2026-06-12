import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { NgClass } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TicketService } from '../../../services/ticket.service';
import { Router } from '@angular/router';
import { Priority, Status, TicketResponse } from '../../../../models/ticket.model';

@Component({
  selector: 'app-ticket-list',
  imports: [NgClass, FormsModule],
  templateUrl: './ticket-list.html',
  styleUrl: './ticket-list.css',
})
export class TicketList implements OnInit {
  constructor(
    private ticketService: TicketService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  tickets: TicketResponse[] = [];
  isLoading: boolean = true;
  keyword: string = '';
  selectedStatus: string = '';
  selectedPriority: string = '';

  statuses = Object.values(Status);
  priorities = Object.values(Priority);

  ngOnInit(): void {
    this.loadTickets();
  }

  loadTickets(): void {
    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        this.tickets = tickets;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load tickets', error);
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  search(): void {
    if (this.keyword) {
      this.ticketService.searchTickets(this.keyword).subscribe({
        next: (response) => {
          this.tickets = response.content;
          this.cdr.detectChanges();
        },
        error: (error) => console.error('Search failed', error),
      });
    } else {
      this.loadTickets();
    }
  }

  filterByStatus(): void {
    if (this.selectedStatus) {
      this.ticketService.getTicketsByStatus(this.selectedStatus).subscribe({
        next: (tickets) => {
          this.tickets = tickets;
          this.cdr.detectChanges();
        },
        error: (error) => console.error('Filter failed', error),
      });
    } else {
      this.loadTickets();
    }
  }

  filterByPriority(): void {
    if (this.selectedPriority) {
      this.ticketService.getTicketsByPriority(this.selectedPriority).subscribe({
        next: (tickets) => {
          this.tickets = tickets;
          this.cdr.detectChanges();
        },
        error: (error) => console.error('Filter failed', error),
      });
    } else {
      this.loadTickets();
    }
  }

  viewTicket(id: number): void {
    this.router.navigate(['/tickets', id]);
  }

  deleteTicket(id: number): void {
    this.ticketService.deleteTicket(id).subscribe({
      next: () => {
        this.tickets = this.tickets.filter((t) => t.id !== id);
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to delete ticket', error),
    });
  }

  activeDropdown: number | null = null;

  toggleDropdown(ticketId: number): void {
    this.activeDropdown = this.activeDropdown === ticketId ? null : ticketId;
  }
}
