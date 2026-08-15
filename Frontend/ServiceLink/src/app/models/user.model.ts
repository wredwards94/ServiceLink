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
  /**
   * Not currently sent. Role lives on the Credentials entity and
   * UserResponseDto does not project it, so this is undefined in every
   * response today — the people list shows a person's role as unknown until
   * the DTO carries it. Declared optional so the UI picks it up automatically
   * if the backend starts sending it.
   */
  role?: Role;
}

export enum Role {
  ADMIN = 'ADMIN',
  AGENT = 'AGENT',
  USER = 'USER',
}
