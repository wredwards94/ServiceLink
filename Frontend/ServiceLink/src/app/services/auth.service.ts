import { Injectable } from '@angular/core';
import fetchFromAPI from "./api.service";
import {UserId} from "../types/types";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor() { }

  async login(username: string, password: string) {
    const payload = { "username": username, "password": password }
    console.log(payload)
    return await fetchFromAPI('POST', `api/users/auth/login`, payload).then((result) => {
      if (result == undefined) {
        console.log('No user found');
        return undefined;
      } else {
        console.log(result);
        return result;
      }
    });
  }
}
