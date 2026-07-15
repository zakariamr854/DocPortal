import { Component, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpEventType } from '@angular/common/http';
import { AuthService } from '../../core/auth.service';
import {
  CategoryDto, DocumentDto, DocumentService, HistoryDto, OwnerDto, PageResult, VersionDto
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

  // Modal versions
  readonly versionsDoc = signal<DocumentDto | null>(null);
  readonly versions = signal<VersionDto[]>([]);
  readonly versionsLoading = signal(false);
  readonly compareSel = signal<VersionDto[]>([]);
  readonly expandedVersions = signal<number[]>([]);
  readonly versionError = signal('');

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

  // ===== Versions =====

  openVersions(doc: DocumentDto) {
    this.versionsDoc.set(doc);
    this.versions.set([]);
    this.compareSel.set([]);
    this.expandedVersions.set([]);
    this.versionError.set('');
    this.loadVersions(doc.id);
  }

  loadVersions(docId: number) {
    this.versionsLoading.set(true);
    this.documents.versions(docId).subscribe({
      next: v => { this.versions.set(v); this.versionsLoading.set(false); },
      error: () => { this.versionsLoading.set(false); this.versionError.set('Impossible de charger les versions.'); }
    });
  }

  toggleExpand(v: VersionDto) {
    const ex = this.expandedVersions();
    this.expandedVersions.set(
      ex.includes(v.versionNumber) ? ex.filter(n => n !== v.versionNumber) : [...ex, v.versionNumber]
    );
  }

  isExpanded(v: VersionDto): boolean {
    return this.expandedVersions().includes(v.versionNumber);
  }

  /** L'utilisateur simple ne télécharge que l'état actuel ; l'admin télécharge toute version. */
  canDownloadVersion(v: VersionDto): boolean {
    return this.isAdmin || v.current;
  }

  /** La restauration d'une version précédente est réservée à l'administrateur. */
  canRestoreVersion(v: VersionDto): boolean {
    return this.isAdmin && !v.current && v.id !== null;
  }

  downloadVersion(v: VersionDto) {
    const doc = this.versionsDoc();
    if (!doc) return;
    // Version actuelle = fichier courant du document ; version précédente = fichier archivé
    const req = v.id === null ? this.documents.download(doc.id) : this.documents.downloadVersion(doc.id, v.id);
    req.subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = v.originalFileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: err => this.versionError.set(err.error?.message ?? 'Échec du téléchargement de la version.')
    });
  }

  restoreVersion(v: VersionDto) {
    const doc = this.versionsDoc();
    if (!doc || v.id === null) return;
    this.documents.restoreVersion(doc.id, v.id).subscribe({
      next: () => {
        this.compareSel.set([]);
        this.flash(`Version v${v.versionNumber} restaurée.`);
        this.loadVersions(doc.id);
        this.load();
      },
      error: err => this.versionError.set(err.error?.message ?? 'Échec de la restauration.')
    });
  }

  toggleCompare(v: VersionDto) {
    const sel = this.compareSel();
    if (sel.some(s => s.versionNumber === v.versionNumber)) {
      this.compareSel.set(sel.filter(s => s.versionNumber !== v.versionNumber));
    } else if (sel.length < 2) {
      this.compareSel.set([...sel, v]);
    } else {
      // Déjà 2 sélectionnées : on remplace la plus ancienne sélection
      this.compareSel.set([sel[1], v]);
    }
  }

  isCompared(v: VersionDto): boolean {
    return this.compareSel().some(s => s.versionNumber === v.versionNumber);
  }

  get comparePair(): [VersionDto, VersionDto] | null {
    const sel = this.compareSel();
    if (sel.length !== 2) return null;
    const sorted = [...sel].sort((a, b) => a.versionNumber - b.versionNumber);
    return [sorted[0], sorted[1]];
  }

  /** Vrai si la valeur a changé entre deux versions (pour surligner la différence). */
  changed(a: string | number | null, b: string | number | null): boolean {
    return (a ?? '') !== (b ?? '');
  }

  visibilityLabel(v: string | null): string {
    return v === 'PUBLIC' ? 'Public' : v === 'PRIVATE' ? 'Privé' : '—';
  }

  sizeDelta(a: VersionDto, b: VersionDto): string {
    const delta = b.size - a.size;
    if (delta === 0) return 'taille identique';
    const sign = delta > 0 ? '+' : '−';
    return `${sign}${this.formatSize(Math.abs(delta))}`;
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
      case 'NEW_VERSION': return 'Nouvelle version';
      default: return action;
    }
  }

  private flash(message: string) {
    this.notice.set(message);
    setTimeout(() => this.notice.set(''), 4000);
  }
}
