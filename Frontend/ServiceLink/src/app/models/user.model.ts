import {TicketResponse} from './ticket.model';

export interface Credentials {
  username: string;
  password: string;
}

export interface Profile {
  firstName: string;
  lastName: string;
  email: string;
}

export interface UserIdResponse {
  userId: string;
  token: string;
  role: Role;
}

export interface UserResponse {
  userId: string;
  profile: Profile;
  assignedTickets: TicketResponse[];
  requestedTickets: TicketResponse[];
}

export enum Role {
  ADMIN = 'ADMIN',
  AGENT = 'AGENT',
  USER = 'USER',
}
