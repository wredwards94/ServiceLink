import { Component } from '@angular/core';
import {TicketNoComments, UserId} from "../../types/types";
import {TicketService} from "../../services/ticket.service";
import { KeyValuePipe } from '@angular/common';
import {of} from "rxjs";
import {ActivatedRoute} from "@angular/router";

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

  constructor(private ticketsService: TicketService, private route: ActivatedRoute) { }

  async ngOnInit() {
    const id = this.route.snapshot.paramMap.get('userId'); // Retrieve the userId from route param
    console.log(`Retrieved userId from route params: ${id}`);

    if (id) {
      await this.ticketsService.fetchUserTickets(id).then(r => {
        this.ticketData = r as any;
        console.log(`ticket data: ${JSON.stringify(this.ticketData)}`);
        for (const ticket of this.ticketData) {
          console.log(ticket);
        }
      }).catch(error => {
        console.error('Error fetching tickets:', error);
      });
    } else {
      console.error('UserId not found in route params');
    }
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
