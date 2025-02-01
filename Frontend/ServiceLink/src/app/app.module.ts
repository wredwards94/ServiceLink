import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { WelcomeScreenComponent } from './components/welcome-screen/welcome-screen.component';
import { LoginComponent } from './components/login/login.component';
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {Button, ButtonDirective} from "primeng/button";
import {Ripple} from "primeng/ripple";
import { TicketPageComponent } from './components/ticket-page/ticket-page.component';
import {TableModule} from "primeng/table";
import { TicketDetailsComponent } from './components/ticket-page/ticket-details/ticket-details.component';

@NgModule({
  declarations: [
    AppComponent,
    WelcomeScreenComponent,
    LoginComponent,
    TicketPageComponent,
    TicketDetailsComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    Button,
    ButtonDirective,
    Ripple,
    TableModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
