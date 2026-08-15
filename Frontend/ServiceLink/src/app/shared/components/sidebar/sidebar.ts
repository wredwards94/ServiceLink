import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { Role } from '../../../models/user.model';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.css',
})
export class Sidebar implements OnInit {
  role: string | null = null;
  isAdmin = false;
  displayName: string | null = null;

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.role = this.authService.getRole();
    this.isAdmin = this.role === Role.ADMIN;

    const userId = this.authService.getUserId();
    if (!userId) return;

    // Identity for the footer. The JWT carries the id and role but no name, so
    // this is the only way to greet someone by it. A failure here is not worth
    // surfacing — the footer falls back to the role alone.
    this.userService.getUserById(userId).subscribe({
      next: (user) => {
        const profile = user?.profile;
        if (profile) {
          this.displayName = `${profile.firstName} ${profile.lastName}`.trim();
        }
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
