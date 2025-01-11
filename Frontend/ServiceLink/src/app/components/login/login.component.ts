import {Component, Input, Output} from '@angular/core';
import {AuthService} from "../../services/auth.service";
import { ButtonModule } from 'primeng/button';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {

  username: string = '';
  password: string = '';

  constructor(private authService: AuthService) {  }

  // setCredentials(username: string, password: string) {
  //   this.username = username;
  //   this.password = password;
  // }
  async login() {
    console.log(this.username, this.password);
    this.authService.login(this.username, this.password).then(r =>
    {
      console.log(r);
      return r;
    });
  }
}
