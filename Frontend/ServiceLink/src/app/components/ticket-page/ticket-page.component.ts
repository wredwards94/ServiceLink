import { Component } from '@angular/core';
import {TicketNoComments} from "../../types/types";
import {TicketService} from "../../services/ticket.service";
import { KeyValuePipe } from '@angular/common';
import {of} from "rxjs";

@Component({
  selector: 'app-ticket-page',
  templateUrl: './ticket-page.component.html',
  styleUrl: './ticket-page.component.css'
})
export class TicketPageComponent {

  ticketData: TicketNoComments[] = [];

  ticket = {
    Id: '',
    Title: '',
    // Description: '',
    Status: '',
    Priority: '',
    Category: '',
    Assigned: '',
    Requester: '',
    'Request Time': '',
    'Updated At': '',
  }

  constructor(private ticketsService: TicketService) { }

  async ngOnInit() {
    this.ticketsService.fetchTickets().then(r => {
      this.ticketData = r as any;
      // this.ticketData.push(r.ticket);
      console.log(`ticket data ${this.ticketData}`);
      for(const ticket of this.ticketData) {
        console.log(ticket)
      }
    }).catch(error => {
      console.log(error);
    });
  }

  returnZero() {
    return 0;
  }

  // async fetchTickets() {
  //   this.ticketsService.fetchTickets().then(r => {
  //     this.ticketData = r as TicketNoComments;
  //   }).catch(error => {
  //     console.log(error);
  //   });
  // }

}
