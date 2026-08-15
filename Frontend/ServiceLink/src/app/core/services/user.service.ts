import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Profile, Role, UserResponse } from '../../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/api/users`;

  constructor(private http: HttpClient) {}

  /** ADMIN only — SecurityConfig restricts GET /api/users to that role. */
  getUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  getUserById(id: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  updateProfile(userId: string, profile: Partial<Profile>): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/profile/${userId}`, profile);
  }

  /** ADMIN only. */
  updateRole(userId: string, role: Role): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.apiUrl}/${userId}/role`, { role });
  }
}
