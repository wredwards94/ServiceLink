import { NgModule } from '@angular/core';
import {RouterModule, Routes, withComponentInputBinding} from '@angular/router';
import {LoginComponent} from "./components/login/login.component";
import {TicketPageComponent} from "./components/ticket-page/ticket-page.component";
import {WelcomeScreenComponent} from "./components/welcome-screen/welcome-screen.component";

const routes: Routes = [
  {
    path: '', redirectTo:'login', pathMatch: 'full',
  },
  {
    path: 'login', component: WelcomeScreenComponent
  },
  {
    path: 'tickets', component: TicketPageComponent
  },
  {
    path: 'tickets/ticketId=', component: TicketPageComponent
  },
  {
    path: '**', redirectTo:'login', pathMatch: 'full'
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
