import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Login } from './core/pages/login/login';
import { Dashboard } from './core/pages/dashboard/dashboard';
import { TicketDetail } from './core/pages/tickets/ticket-detail/ticket-detail';
import { TicketList } from './core/pages/tickets/ticket-list/ticket-list';
import { AdminUsers } from './core/pages/admin/users/users';
import { Role } from './models/user.model';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'dashboard', component: Dashboard, canActivate: [authGuard] },
  { path: 'tickets', component: TicketList, canActivate: [authGuard] },
  { path: 'tickets/:id', component: TicketDetail, canActivate: [authGuard] },
  {
    // The sidebar has always linked here; the route did not exist, so the
    // wildcard below caught it and bounced admins back to the login screen.
    path: 'admin/users',
    component: AdminUsers,
    canActivate: [authGuard, roleGuard([Role.ADMIN])],
  },
  { path: '**', redirectTo: 'login' },
];
