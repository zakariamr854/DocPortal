import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CategoryDto, DocumentService } from '../../core/document.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './categories.html',
  styleUrl: '../documents/documents.css'
})
export class CategoriesComponent {

  readonly categories = signal<CategoryDto[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly notice = signal('');


  readonly editing = signal<CategoryDto | null>(null);
  readonly showForm = signal(false);
  formName = '';
  formDescription = '';

  readonly deleteTarget = signal<CategoryDto | null>(null);

  constructor(private documents: DocumentService) {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.documents.categories().subscribe({
      next: cats => { this.categories.set(cats); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('Impossible de charger les catégories.'); }
    });
  }

  openCreate() {
    this.editing.set(null);
    this.formName = '';
    this.formDescription = '';
    this.showForm.set(true);
  }

  openEdit(cat: CategoryDto) {
    this.editing.set(cat);
    this.formName = cat.name;
    this.formDescription = cat.description ?? '';
    this.showForm.set(true);
  }

  submitForm() {
    if (!this.formName.trim()) {
      this.error.set('Le nom de la catégorie est obligatoire.');
      return;
    }
    const editing = this.editing();
    const request = editing
      ? this.documents.updateCategory(editing.id, { name: this.formName.trim(), description: this.formDescription })
      : this.documents.createCategory({ name: this.formName.trim(), description: this.formDescription });

    request.subscribe({
      next: () => {
        this.showForm.set(false);
        this.flash(editing ? 'Catégorie modifiée.' : 'Catégorie créée.');
        this.load();
      },
      error: err => this.error.set(err.error?.message ?? "Échec de l'enregistrement.")
    });
  }

  toggleActive(cat: CategoryDto) {
    this.documents.updateCategory(cat.id, { active: !cat.active }).subscribe({
      next: () => { this.flash(cat.active ? 'Catégorie désactivée.' : 'Catégorie réactivée.'); this.load(); },
      error: err => this.error.set(err.error?.message ?? 'Échec de la mise à jour.')
    });
  }

  confirmDelete() {
    const cat = this.deleteTarget();
    if (!cat) return;
    this.documents.deleteCategory(cat.id).subscribe({
      next: () => {
        this.deleteTarget.set(null);
        this.flash(cat.documentCount > 0
          ? 'Catégorie utilisée par des documents : elle a été désactivée.'
          : 'Catégorie supprimée.');
        this.load();
      },
      error: err => {
        this.deleteTarget.set(null);
        this.error.set(err.error?.message ?? 'Échec de la suppression.');
      }
    });
  }

  private flash(message: string) {
    this.notice.set(message);
    setTimeout(() => this.notice.set(''), 4000);
  }
}
