import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginComponent {

  username = '';
  password = '';
  rememberMe = false;
  hidePassword = true;

  readonly loading = signal(false);
  readonly error = signal('');

  constructor(private auth: AuthService, private router: Router) {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login() {
    if (this.loading()) {
      return;
    }

    const username = this.username.trim();
    if (!username || !this.password) {
      this.error.set("Veuillez saisir votre nom d'utilisateur et votre mot de passe.");
      return;
    }

    this.loading.set(true);
    this.error.set('');

    this.auth.login({ username, password: this.password }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: err => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 400) {
          this.error.set(err.error?.message ?? 'Identifiants incorrects');
        } else {
          this.error.set('Impossible de contacter le serveur. Veuillez réessayer.');
        }
      }
    });
  }
}
