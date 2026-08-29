import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth.guard';
import { LoginComponent } from './pages/login/login';
import { ShellComponent } from './pages/shell/shell';
import { DashboardComponent } from './pages/dashboard/dashboard';
import { DocumentsComponent } from './pages/documents/documents';
import { CategoriesComponent } from './pages/categories/categories';
import { TrashComponent } from './pages/trash/trash';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'documents', component: DocumentsComponent },
      { path: 'categories', component: CategoriesComponent, canActivate: [adminGuard] },
      { path: 'trash', component: TrashComponent, canActivate: [adminGuard] }
    ]
  },
  { path: '**', redirectTo: 'login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
