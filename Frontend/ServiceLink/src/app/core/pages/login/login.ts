import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { UserIdResponse } from '../../../models/user.model';
import { Role } from '../../../models/user.model';
import { Credentials } from '../../../models/user.model';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login implements OnInit {
  ngOnInit(): void {}

  constructor(
    private authService: AuthService,
    private router: Router,
  ) {}
  username: string = '';
  password: string = '';

  credentials: Credentials = {
    username: '',
    password: '',
  };

  userToken: UserIdResponse = {
    userId: '',
    token: '',
    role: Role.USER,
  };

  onSubmit(): void {
    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        console.log('Login response:', response);
        console.log('Token stored:', localStorage.getItem('token'));
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        console.error('Login failed', error);
      },
    });
  }
}
