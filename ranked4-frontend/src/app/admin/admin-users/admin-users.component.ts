import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { map, Observable } from 'rxjs';
import { AdminUsersService } from './admin-users.service';
import { ApiUserProfile, ApiUsersResponse } from './admin-users.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss'
})
export class AdminUsersComponent implements OnInit {
  private adminUsersService = inject(AdminUsersService);

  public users$!: Observable<ApiUserProfile[]>;
  public currentPage = 0;
  public totalPages = 0;
  public pageSize = 10;

  public editingUser: WritableSignal<ApiUserProfile | null> = signal(null);
  public showEditModal: WritableSignal<boolean> = signal(false);
  public showDeleteConfirm: WritableSignal<boolean> = signal(false);
  public userToDelete: WritableSignal<ApiUserProfile | null> = signal(null);

  public goldAmount = 0;
  public selectedDiscCode = '';

  ngOnInit(): void {
    this.loadUsers(this.currentPage);
  }

  loadUsers(page: number): void {
    this.currentPage = page;
    this.users$ = this.adminUsersService.getUserList(page, this.pageSize, 'id,desc').pipe(
      map((response: ApiUsersResponse) => {
        this.totalPages = response.totalPages;
        return response.content;
      })
    );
  }

  openEditModal(user: ApiUserProfile): void {
    this.editingUser.set({ ...user });
    this.goldAmount = 0;
    this.selectedDiscCode = '';
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingUser.set(null);
    this.goldAmount = 0;
    this.selectedDiscCode = '';
  }

  creditGold(): void {
    const user = this.editingUser();
    if (!user || this.goldAmount <= 0) return;

    this.adminUsersService.creditGold(user.userId, this.goldAmount).subscribe({
      next: () => {
        this.loadUsers(this.currentPage);
        this.goldAmount = 0;
      },
      error: (err) => console.error('Error crediting gold:', err)
    });
  }

  addDisc(): void {
    const user = this.editingUser();
    if (!user || !this.selectedDiscCode) return;

    this.adminUsersService.addDiscToUser(user.userId, this.selectedDiscCode, false).subscribe({
      next: (updatedUser) => {
        this.editingUser.set(updatedUser);
        this.loadUsers(this.currentPage);
        this.selectedDiscCode = '';
      },
      error: (err) => console.error('Error adding disc:', err)
    });
  }

  removeDisc(itemCode: string): void {
    const user = this.editingUser();
    if (!user) return;

    this.adminUsersService.removeDiscFromUser(user.userId, itemCode).subscribe({
      next: (updatedUser) => {
        this.editingUser.set(updatedUser);
        this.loadUsers(this.currentPage);
      },
      error: (err) => console.error('Error removing disc:', err)
    });
  }

  openDeleteConfirm(user: ApiUserProfile): void {
    this.userToDelete.set(user);
    this.showDeleteConfirm.set(true);
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirm.set(false);
    this.userToDelete.set(null);
  }

  confirmDelete(): void {
    const user = this.userToDelete();
    if (!user) return;

    this.adminUsersService.deleteUser(user.userId).subscribe({
      next: () => {
        this.loadUsers(this.currentPage);
        this.closeDeleteConfirm();
      },
      error: (err) => console.error('Error deleting user:', err)
    });
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.loadUsers(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.loadUsers(this.currentPage + 1);
    }
  }
}
