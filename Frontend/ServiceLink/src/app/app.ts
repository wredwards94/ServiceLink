import { Component, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { Login } from './core/pages/login/login';
import { Sidebar } from './shared/components/sidebar/sidebar';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Login, Sidebar],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  showSidebar = false;

  constructor(private router: Router) {
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        this.showSidebar = event.url !== '/login' && event.url !== '/';
      });
  }
}
