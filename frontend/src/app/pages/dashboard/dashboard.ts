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

  get isViewer(): boolean {
    return this.auth.currentUser()?.role === 'VIEWER';
  }

  /** Nombre de catégories contenant au moins un document consultable. */
  get categoriesWithDocs(): number {
    return this.stats()?.documentsPerCategory.filter(c => c.count > 0).length ?? 0;
  }

  /** Date du document le plus récent que l'utilisateur peut consulter. */
  get lastDocumentDate(): string | null {
    const recent = this.stats()?.recentDocuments;
    return recent && recent.length ? recent[0].createdAt : null;
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
      case 'NEW_VERSION': return 'Nouvelle version';
      default: return action;
    }
  }
}
