import {Component, Input, Output} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  username: string = '';
  password: string = '';

  constructor(private authService: AuthService, private router: Router) {  }

  // setCredentials(username: string, password: string) {
  //   this.username = username;
  //   this.password = password;
  // }
  async login() {
    try {
      const result = await this.authService.login(this.username, this.password);
      if (result === undefined) {
        alert('Login failed');
      } else {
        console.log('Login successful');
        console.log(`result ${result.userId}`);
        await this.router.navigate([`/tickets/${result.userId}`]);
      }
    } catch (error) {
      console.error('Login error:', error);
      alert('An error occurred during login');
    }
  }
}
