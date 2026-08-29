import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DocumentDto, DocumentService } from '../../core/document.service';

@Component({
  selector: 'app-trash',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './trash.html',
  styleUrl: '../documents/documents.css'
})
export class TrashComponent {

  readonly docs = signal<DocumentDto[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');

  readonly purgeTarget = signal<DocumentDto | null>(null);

  constructor(private documents: DocumentService) {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.documents.trash().subscribe({
      next: docs => { this.docs.set(docs); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger la corbeille.'); }
    });
  }

  restore(doc: DocumentDto) {
    this.documents.untrash(doc.id).subscribe({
      next: () => { this.flash(`« ${doc.title} » restauré.`); this.load(); },
      error: err => this.error.set(err.error?.message ?? 'Échec de la restauration.')
    });
  }

  confirmPurge() {
    const doc = this.purgeTarget();
    if (!doc) return;
    this.documents.purge(doc.id).subscribe({
      next: () => {
        this.purgeTarget.set(null);
        this.flash(`« ${doc.title} » supprimé définitivement.`);
        this.load();
      },
      error: err => {
        this.purgeTarget.set(null);
        this.error.set(err.error?.message ?? 'Échec de la suppression définitive.');
      }
    });
  }

  formatSize(bytes: number | null): string {
    if (bytes === null || bytes === undefined) return '—';
    if (bytes < 1024) return bytes + ' o';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' Ko';
    return (bytes / (1024 * 1024)).toFixed(1) + ' Mo';
  }

  extBadgeClass(ext: string | null): string {
    switch ((ext ?? '').toLowerCase()) {
      case 'pdf': return 'bg-red-100 text-red-700';
      case 'docx': return 'bg-blue-100 text-blue-700';
      case 'xlsx': return 'bg-green-100 text-green-700';
      case 'png':
      case 'jpg':
      case 'jpeg': return 'bg-purple-100 text-purple-700';
      case 'txt': return 'bg-slate-200 text-slate-700';
      default: return 'bg-slate-100 text-slate-600';
    }
  }

  private flash(message: string) {
    this.notice.set(message);
    setTimeout(() => this.notice.set(''), 4000);
  }
}
