import { HttpClient, HttpEvent, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from './auth.service';

export interface DocumentDto {
  id: number;
  title: string;
  originalFileName: string;
  extension: string;
  mimeType: string;
  size: number;
  description: string | null;
  tags: string | null;
  visibility: 'PUBLIC' | 'PRIVATE';
  status: 'ACTIVE' | 'ARCHIVED' | 'DELETED';
  categoryId: number | null;
  categoryName: string | null;
  ownerId: number | null;
  ownerName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryDto {
  id: number;
  name: string;
  description: string | null;
  active: boolean;
  documentCount: number;
}

export interface HistoryDto {
  id: number;
  documentId: number;
  documentName: string;
  userName: string;
  action: string;
  comment: string | null;
  actionDate: string;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface DashboardStats {
  totalDocuments: number;
  myDocuments: number;
  activeCategories: number;
  storageUsedBytes: number;
  documentsPerCategory: { id: number; name: string; count: number }[];
  recentDocuments: DocumentDto[];
  recentActions: HistoryDto[];
}

export interface OwnerDto {
  id: number;
  name: string;
}

export interface VersionDto {
  id: number | null;
  versionNumber: number;
  originalFileName: string;
  extension: string;
  size: number;
  title: string | null;
  description: string | null;
  tags: string | null;
  visibility: string | null;
  categoryName: string | null;
  comment: string | null;
  uploadedBy: string | null;
  createdAt: string;
  current: boolean;
}

export interface DocumentFilters {
  q?: string;
  categoryId?: number | null;
  extension?: string;
  status?: string;
  mine?: boolean;
  ownerId?: number | null;
  page?: number;
  size?: number;
  sort?: string;
  dir?: string;
}

export interface UploadMetadata {
  title?: string;
  description?: string;
  tags?: string;
  categoryId: number;
  visibility?: string;
}

@Injectable({ providedIn: 'root' })
export class DocumentService {

  constructor(private http: HttpClient) {}

  list(filters: DocumentFilters): Observable<PageResult<DocumentDto>> {
    let params = new HttpParams();
    if (filters.q) params = params.set('q', filters.q);
    if (filters.categoryId) params = params.set('categoryId', filters.categoryId);
    if (filters.extension) params = params.set('extension', filters.extension);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.mine) params = params.set('mine', true);
    if (filters.ownerId) params = params.set('ownerId', filters.ownerId);
    params = params.set('page', filters.page ?? 0);
    params = params.set('size', filters.size ?? 10);
    if (filters.sort) params = params.set('sort', filters.sort);
    if (filters.dir) params = params.set('dir', filters.dir);
    return this.http.get<PageResult<DocumentDto>>(`${API_URL}/documents`, { params });
  }

  upload(file: File, meta: UploadMetadata): Observable<HttpEvent<DocumentDto>> {
    const form = new FormData();
    form.append('file', file);
    form.append('categoryId', String(meta.categoryId));
    if (meta.title) form.append('title', meta.title);
    if (meta.description) form.append('description', meta.description);
    if (meta.tags) form.append('tags', meta.tags);
    if (meta.visibility) form.append('visibility', meta.visibility);
    return this.http.post<DocumentDto>(`${API_URL}/documents/upload`, form, {
      reportProgress: true,
      observe: 'events'
    });
  }

  download(id: number): Observable<Blob> {
    return this.http.get(`${API_URL}/documents/${id}/download`, { responseType: 'blob' });
  }

  detail(id: number): Observable<DocumentDto> {
    return this.http.get<DocumentDto>(`${API_URL}/documents/${id}`);
  }

  history(id: number): Observable<HistoryDto[]> {
    return this.http.get<HistoryDto[]>(`${API_URL}/documents/${id}/history`);
  }

  update(id: number, changes: Partial<UploadMetadata & { visibility: string }>): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${API_URL}/documents/${id}`, changes);
  }

  delete(id: number): Observable<unknown> {
    return this.http.delete(`${API_URL}/documents/${id}`);
  }

  archive(id: number): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${API_URL}/documents/${id}/archive`, {});
  }

  restore(id: number): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${API_URL}/documents/${id}/restore`, {});
  }

  stats(): Observable<DashboardStats> {
    return this.http.get<DashboardStats>(`${API_URL}/documents/stats`);
  }

  categories(): Observable<CategoryDto[]> {
    return this.http.get<CategoryDto[]>(`${API_URL}/categories`);
  }

  owners(): Observable<OwnerDto[]> {
    return this.http.get<OwnerDto[]>(`${API_URL}/documents/owners`);
  }



  trash(): Observable<DocumentDto[]> {
    return this.http.get<DocumentDto[]>(`${API_URL}/documents/trash`);
  }

  untrash(id: number): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${API_URL}/documents/${id}/untrash`, {});
  }

  purge(id: number): Observable<unknown> {
    return this.http.delete(`${API_URL}/documents/${id}/purge`);
  }

  versions(docId: number): Observable<VersionDto[]> {
    return this.http.get<VersionDto[]>(`${API_URL}/documents/${docId}/versions`);
  }

  addVersion(docId: number, file: File, comment: string): Observable<HttpEvent<DocumentDto>> {
    const form = new FormData();
    form.append('file', file);
    if (comment) form.append('comment', comment);
    return this.http.post<DocumentDto>(`${API_URL}/documents/${docId}/versions`, form, {
      reportProgress: true,
      observe: 'events'
    });
  }

  downloadVersion(docId: number, versionId: number): Observable<Blob> {
    return this.http.get(`${API_URL}/documents/${docId}/versions/${versionId}/download`, { responseType: 'blob' });
  }

  restoreVersion(docId: number, versionId: number): Observable<DocumentDto> {
    return this.http.put<DocumentDto>(`${API_URL}/documents/${docId}/versions/${versionId}/restore`, {});
  }

  createCategory(category: { name: string; description?: string }): Observable<CategoryDto> {
    return this.http.post<CategoryDto>(`${API_URL}/categories`, category);
  }

  updateCategory(id: number, category: { name?: string; description?: string; active?: boolean }): Observable<CategoryDto> {
    return this.http.put<CategoryDto>(`${API_URL}/categories/${id}`, category);
  }

  deleteCategory(id: number): Observable<unknown> {
    return this.http.delete(`${API_URL}/categories/${id}`);
  }
}
