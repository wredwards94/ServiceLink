import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { TicketService } from '../../services/ticket.service';
import { AuthService } from '../../services/auth.service';
import { TicketResponse, Status, Priority } from '../../../models/ticket.model';


@Component({
  selector: 'app-dashboard',
  imports: [NgClass],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard implements OnInit {
  constructor(
    private ticketService: TicketService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  tickets: TicketResponse[] = [];
  totalTickets: number = 0;
  openTickets: number = 0;
  criticalTickets: number = 0;
  inProgressTickets: number = 0;
  isLoading: boolean = true;
  activeDropdown: number | null = null;

  ngOnInit(): void {
    console.log('Dashboard loaded');
    this.loadTickets();
  }

  loadTickets(): void {
    this.ticketService.getAllTickets().subscribe({
      next: (tickets) => {
        console.log('Tickets received:', tickets);
        this.tickets = tickets;
        this.totalTickets = tickets.length;
        this.openTickets = tickets.filter((t) => t.status === Status.NEW).length;
        this.criticalTickets = tickets.filter((t) => t.priority === Priority.CRITICAL).length;
        this.inProgressTickets = tickets.filter((t) => t.status === Status.IN_PROGRESS).length;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load tickets', error);
        console.error('Status:', error.status);
        console.error('Message:', error.message);
        this.isLoading = false;
      },
    });
  }

  toggleDropdown(ticketId: number): void {
    this.activeDropdown = this.activeDropdown === ticketId ? null : ticketId;
  }

  viewTicket(ticketId: number): void {
    this.router.navigate(['/tickets', ticketId]);
  }

  deleteTicket(ticketId: number): void {
    this.ticketService.deleteTicket(ticketId).subscribe({
      next: () => {
        this.tickets = this.tickets.filter((t) => t.id !== ticketId);
        this.totalTickets = this.tickets.length;
        this.cdr.detectChanges();
      },
      error: (error) => console.error('Failed to delete ticket', error),
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
