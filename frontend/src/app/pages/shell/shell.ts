import { Component } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.css'
})
export class ShellComponent {

  readonly user;

  constructor(private auth: AuthService, private router: Router) {
    this.user = this.auth.currentUser;
    this.auth.me().subscribe({ error: () => {} });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
