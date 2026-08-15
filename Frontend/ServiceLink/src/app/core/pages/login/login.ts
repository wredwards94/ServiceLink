import { ChangeDetectorRef, Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { Credentials } from '../../../models/user.model';
import { Status } from '../../../models/ticket.model';
import { LifecycleRail } from '../../../shared/components/lifecycle-rail/lifecycle-rail';

@Component({
  selector: 'app-login',
  imports: [FormsModule, LifecycleRail],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  credentials: Credentials = { username: '', password: '' };
  isSubmitting = false;
  error: string | null = null;

  /** The rail on the sign-in panel is decorative-static; a mid-line state
      shows the most of the machine at once. */
  readonly sampleStatus = Status.IN_PROGRESS;

  onSubmit(): void {
    if (this.isSubmitting) return;
    if (!this.credentials.username || !this.credentials.password) {
      this.error = 'Enter your username and password.';
      return;
    }

    this.isSubmitting = true;
    this.error = null;

    this.authService.login(this.credentials).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.error =
          err.status === 401 || err.status === 403
            ? 'That username and password do not match an account.'
            : 'Cannot reach the service desk right now. Try again in a moment.';
        this.cdr.detectChanges();
      },
    });
  }
}
