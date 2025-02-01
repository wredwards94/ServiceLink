import { Injectable } from '@angular/core';
import fetchFromAPI from "./api.service";

@Injectable({
  providedIn: 'root'
})
export class TicketService {

  constructor() { }

  async fetchTickets() {
    return await fetchFromAPI('GET', 'api/tickets');
  }
}
