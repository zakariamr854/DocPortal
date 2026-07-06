import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

export interface UserDto {
  id: number;
  username: string;
  fullName: string | null;
  email: string;
  role: 'ADMIN' | 'USER' | 'VIEWER';
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
}

export const API_URL = 'http://localhost:8080/api';

const TOKEN_KEY = 'docportal_token';
const USER_KEY = 'docportal_user';

@Injectable({ providedIn: 'root' })
export class AuthService {

  readonly currentUser = signal<UserDto | null>(this.readStoredUser());

  constructor(private http: HttpClient) {}

  login(credentials: { username: string; password: string }): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_URL}/auth/login`, credentials).pipe(
      tap(res => {
        localStorage.setItem(TOKEN_KEY, res.token);
        localStorage.setItem(USER_KEY, JSON.stringify(res.user));
        this.currentUser.set(res.user);
      })
    );
  }

  me(): Observable<UserDto> {
    return this.http.get<UserDto>(`${API_URL}/auth/me`).pipe(
      tap(user => {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.currentUser.set(user);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.currentUser.set(null);
  }

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return this.token !== null;
  }

  private readStoredUser(): UserDto | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) as UserDto : null;
    } catch {
      return null;
    }
  }
}
