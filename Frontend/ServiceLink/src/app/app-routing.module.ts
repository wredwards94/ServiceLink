import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import {LoginComponent} from "./components/login/login.component";
// import {TicketPageComponent} from "./components/ticket-page/ticket-page.component";
import {WelcomeScreenComponent} from "./components/welcome-screen/welcome-screen.component";

const routes: Routes = [
  {
    path: '', component: WelcomeScreenComponent
  },
  // {
  //   path: 'login', component: LoginComponent,
  //   // pathMatch: 'full',
  // },
  {
    // path: 'tickets', component: TicketPageComponent
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
