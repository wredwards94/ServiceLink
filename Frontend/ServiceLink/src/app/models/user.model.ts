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

export enum Role {
  ADMIN = 'Admin',
  AGENT = 'Agent',
  USER = 'User',
}
