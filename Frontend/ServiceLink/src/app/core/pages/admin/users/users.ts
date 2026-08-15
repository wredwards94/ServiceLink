import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { UserService } from '../../../services/user.service';
import { AuthService } from '../../../services/auth.service';
import { Role, UserResponse } from '../../../../models/user.model';
import { OPEN_STATUSES } from '../../../../models/ticket.model';

interface PersonRow {
  userId: string;
  name: string;
  email: string;
  role: Role | null;
  openAssigned: number;
  totalAssigned: number;
  requested: number;
  isSelf: boolean;
}

@Component({
  selector: 'app-admin-users',
  imports: [FormsModule],
  templateUrl: './users.html',
})
export class AdminUsers implements OnInit {
  constructor(
    private userService: UserService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  rows: PersonRow[] = [];
  isLoading = true;
  loadError: string | null = null;
  actionError: string | null = null;
  savingUserId: string | null = null;

  roles = Object.values(Role);

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    this.loadError = null;

    this.userService.getUsers().subscribe({
      next: (users) => {
        const me = this.authService.getUserId();
        this.rows = (users ?? []).map((user) => this.toRow(user, me));
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Failed to load users', error);
        this.loadError =
          error.status === 403
            ? 'Only administrators can see the people list.'
            : 'Could not load people. Check that the service desk is running.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  /* UserResponseDto carries the person's tickets inline, so the workload
     columns need no extra request. `role` is not on the DTO — it lives on the
     Credentials entity — so it reads as unknown until the backend exposes it. */
  private toRow(user: UserResponse, me: string | null): PersonRow {
    const assigned = user.assignedTickets ?? [];
    return {
      userId: user.userId,
      name: user.profile
        ? `${user.profile.firstName} ${user.profile.lastName}`.trim()
        : 'Unnamed',
      email: user.profile?.email ?? '—',
      role: user.role ?? null,
      openAssigned: assigned.filter((t) => OPEN_STATUSES.includes(t.status)).length,
      totalAssigned: assigned.length,
      requested: (user.requestedTickets ?? []).length,
      isSelf: !!me && me === user.userId,
    };
  }

  changeRole(row: PersonRow, role: Role): void {
    if (!role || this.savingUserId) return;

    this.savingUserId = row.userId;
    this.actionError = null;

    this.userService.updateRole(row.userId, role).subscribe({
      next: () => {
        row.role = role;
        this.savingUserId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Failed to change role', err);
        this.actionError = err.error?.message ?? `Could not change the role for ${row.name}.`;
        this.savingUserId = null;
        this.cdr.detectChanges();
      },
    });
  }
}
