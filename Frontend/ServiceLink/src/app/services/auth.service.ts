import { Injectable } from '@angular/core';
import { HttpClient } from "@angular/common/http";
import fetchFromAPI from "./api.service";

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  constructor() { }

  async login(username: string, password: string) {
    const payload = { "username": username, "password": password }
    console.log(payload)
    await fetchFromAPI('POST', `api/users/auth/login`, payload).then((result: any)=> {
      if(result == undefined){
        console.log('No user found')
      }
      else{
        console.log(result)
      }
    })
  }
}
