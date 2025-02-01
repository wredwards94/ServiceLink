import { Injectable } from '@angular/core';
import fetchFromAPI from "./api.service";
import {TicketNoComments} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class TicketService {

  constructor() { }

  async fetchTickets() {
    return await fetchFromAPI('GET', 'api/tickets');
  }

  async fetchUserTickets(id: string) {
    return await fetchFromAPI('GET', `api/tickets/assigned/${id}`);
  }
}
