import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { Login } from './core/pages/login/login';
import { Dashboard } from './core/pages/dashboard/dashboard';
import { TicketDetail } from './core/pages/tickets/ticket-detail/ticket-detail';
import { TicketList } from './core/pages/tickets/ticket-list/ticket-list';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
  { path: 'tickets', component: TicketList, canActivate: [authGuard] },
  { path: 'tickets/:id', component: TicketDetail, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
