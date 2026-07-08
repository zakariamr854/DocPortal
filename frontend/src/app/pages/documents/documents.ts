import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpEventType } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';
import {
  CategoryDto, DocumentDto, DocumentService, HistoryDto, OwnerDto, PageResult
} from '../../core/document.service';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [FormsModule, DatePipe],
  templateUrl: './documents.html',
  styleUrl: './documents.css'
})
export class DocumentsComponent {

  // Filtres
  q = '';
  categoryId: number | null = null;
  extension = '';
  status = 'ACTIVE';
  mine = false;
  ownerId: number | null = null;
  sort = 'createdAt';
  dir = 'desc';
  page = 0;
  readonly pageSize = 10;

  readonly result = signal<PageResult<DocumentDto> | null>(null);
  readonly categories = signal<CategoryDto[]>([]);
  readonly owners = signal<OwnerDto[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');

  // Modal upload
  readonly showUpload = signal(false);
  readonly dragOver = signal(false);
  uploadFile: File | null = null;
  uploadTitle = '';
  uploadDescription = '';
  uploadTags = '';
  uploadCategoryId: number | null = null;
  uploadVisibility = 'PRIVATE';
  readonly uploadProgress = signal<number | null>(null);
  readonly uploadError = signal('');

  // Modal édition
  readonly editDoc = signal<DocumentDto | null>(null);
  editTitle = '';
  editDescription = '';
  editTags = '';
  editCategoryId: number | null = null;
  editVisibility = 'PRIVATE';

  // Modal détails
  readonly detailDoc = signal<DocumentDto | null>(null);
  readonly detailHistory = signal<HistoryDto[]>([]);

  // Confirmation de suppression
  readonly deleteTarget = signal<DocumentDto | null>(null);

  constructor(private documents: DocumentService, private auth: AuthService) {
    this.loadCategories();
    if (this.isViewer) {
      this.loadOwners();
    }
    this.load();
  }

  get isAdmin(): boolean {
    return this.auth.currentUser()?.role === 'ADMIN';
  }

  get isViewer(): boolean {
    return this.auth.currentUser()?.role === 'VIEWER';
  }

  loadCategories() {
    this.documents.categories().subscribe({
      next: cats => this.categories.set(cats),
      error: () => {}
    });
  }

  loadOwners() {
    this.documents.owners().subscribe({
      next: owners => this.owners.set(owners),
      error: () => {}
    });
  }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.documents.list({
      q: this.q, categoryId: this.categoryId, extension: this.extension,
      status: this.status, mine: this.mine, ownerId: this.ownerId,
      page: this.page, size: this.pageSize, sort: this.sort, dir: this.dir
    }).subscribe({
      next: res => { this.result.set(res); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger les documents.'); }
    });
  }

  applyFilters() {
    this.page = 0;
    this.load();
  }

  resetFilters() {
    this.q = '';
    this.categoryId = null;
    this.extension = '';
    this.status = 'ACTIVE';
    this.mine = false;
    this.ownerId = null;
    this.applyFilters();
  }

  sortBy(column: string) {
    if (this.sort === column) {
      this.dir = this.dir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sort = column;
      this.dir = 'asc';
    }
    this.load();
  }

  goTo(page: number) {
    const total = this.result()?.totalPages ?? 0;
    if (page < 0 || page >= total) return;
    this.page = page;
    this.load();
  }

  // ===== Upload =====

  openUpload() {
    this.uploadFile = null;
    this.uploadTitle = '';
    this.uploadDescription = '';
    this.uploadTags = '';
    this.uploadCategoryId = null;
    this.uploadVisibility = 'PRIVATE';
    this.uploadProgress.set(null);
    this.uploadError.set('');
    this.showUpload.set(true);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file) this.pickFile(file);
  }

  onFileInput(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.pickFile(file);
  }

  private pickFile(file: File) {
    this.uploadFile = file;
    if (!this.uploadTitle) {
      this.uploadTitle = file.name.replace(/\.[^.]+$/, '');
    }
    this.uploadError.set('');
  }

  removeFile() {
    this.uploadFile = null;
  }

  fileExt(name: string): string {
    const dot = name.lastIndexOf('.');
    return dot >= 0 ? name.slice(dot + 1) : '?';
  }

  submitUpload() {
    if (!this.uploadFile) {
      this.uploadError.set('Veuillez sélectionner un fichier.');
      return;
    }
    if (!this.uploadCategoryId) {
      this.uploadError.set('La catégorie est obligatoire.');
      return;
    }

    this.uploadProgress.set(0);
    this.uploadError.set('');

    this.documents.upload(this.uploadFile, {
      title: this.uploadTitle,
      description: this.uploadDescription,
      tags: this.uploadTags,
      categoryId: this.uploadCategoryId,
      visibility: this.uploadVisibility
    }).subscribe({
      next: event => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.uploadProgress.set(Math.round((event.loaded / event.total) * 100));
        } else if (event.type === HttpEventType.Response) {
          this.showUpload.set(false);
          this.uploadProgress.set(null);
          this.flash(`« ${this.uploadFile!.name} » uploadé avec succès.`);
          this.applyFilters();
          this.loadCategories();
        }
      },
      error: err => {
        this.uploadProgress.set(null);
        this.uploadError.set(err.error?.message ?? "Échec de l'upload.");
      }
    });
  }

  // ===== Actions =====

  downloadDoc(doc: DocumentDto) {
    this.documents.download(doc.id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.originalFileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.error.set('Échec du téléchargement.')
    });
  }

  openDetail(doc: DocumentDto) {
    this.detailDoc.set(doc);
    this.detailHistory.set([]);
    this.documents.history(doc.id).subscribe({
      next: h => this.detailHistory.set(h),
      error: () => {}
    });
  }

  openEdit(doc: DocumentDto) {
    this.editDoc.set(doc);
    this.editTitle = doc.title;
    this.editDescription = doc.description ?? '';
    this.editTags = doc.tags ?? '';
    this.editCategoryId = doc.categoryId;
    this.editVisibility = doc.visibility;
  }

  submitEdit() {
    const doc = this.editDoc();
    if (!doc) return;
    this.documents.update(doc.id, {
      title: this.editTitle,
      description: this.editDescription,
      tags: this.editTags,
      categoryId: this.editCategoryId ?? undefined,
      visibility: this.editVisibility
    }).subscribe({
      next: () => {
        this.editDoc.set(null);
        this.flash('Document modifié.');
        this.load();
      },
      error: err => this.error.set(err.error?.message ?? 'Échec de la modification.')
    });
  }

  archiveDoc(doc: DocumentDto) {
    this.documents.archive(doc.id).subscribe({
      next: () => { this.flash(`« ${doc.title} » archivé.`); this.load(); },
      error: err => this.error.set(err.error?.message ?? "Échec de l'archivage.")
    });
  }

  restoreDoc(doc: DocumentDto) {
    this.documents.restore(doc.id).subscribe({
      next: () => { this.flash(`« ${doc.title} » restauré.`); this.load(); },
      error: err => this.error.set(err.error?.message ?? 'Échec de la restauration.')
    });
  }

  confirmDelete() {
    const doc = this.deleteTarget();
    if (!doc) return;
    this.documents.delete(doc.id).subscribe({
      next: () => {
        this.deleteTarget.set(null);
        this.flash(`« ${doc.title} » supprimé.`);
        this.load();
        this.loadCategories();
      },
      error: err => {
        this.deleteTarget.set(null);
        this.error.set(err.error?.message ?? 'Échec de la suppression.');
      }
    });
  }

  canManage(doc: DocumentDto): boolean {
    if (this.isViewer) {
      return false;
    }
    return this.isAdmin || doc.ownerId === this.auth.currentUser()?.id;
  }

  // ===== Affichage =====

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

  private flash(message: string) {
    this.notice.set(message);
    setTimeout(() => this.notice.set(''), 4000);
  }
}
