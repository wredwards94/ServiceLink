import {Component, Input, Output} from '@angular/core';
import {AuthService} from "../../services/auth.service";

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

  public login(username: string, password: string) {
    console.log(username, password);
    this.authService.login(username, password).then(r =>
    {
      console.log(r);
    });
  }
}
