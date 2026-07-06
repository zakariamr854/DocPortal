import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { DashboardStats, DocumentService } from '../../core/document.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent {

  readonly user;
  readonly stats = signal<DashboardStats | null>(null);

  constructor(private auth: AuthService, private documents: DocumentService) {
    this.user = this.auth.currentUser;
    this.documents.stats().subscribe({
      next: s => this.stats.set(s),
      error: () => {}
    });
  }

  formatSize(bytes: number | null | undefined): string {
    if (bytes === null || bytes === undefined) return '—';
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  actionLabel(action: string): string {
    switch (action) {
      case 'UPLOAD': return 'Upload';
      case 'DOWNLOAD': return 'Téléchargement';
      case 'UPDATE': return 'Modification';
      case 'DELETE': return 'Suppression';
      case 'ARCHIVE': return 'Archivage';
      case 'RESTORE': return 'Restauration';
      default: return action;
    }
  }
}
