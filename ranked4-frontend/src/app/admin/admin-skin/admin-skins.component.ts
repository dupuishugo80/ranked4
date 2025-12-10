import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { map, Observable } from 'rxjs';
import { AdminSkinsService } from './admin-skins.service';
import { ApiDiscsResponse, DiscCustomization, CreateDiscRequest } from './admin-skins.model';

@Component({
  selector: 'app-admin-skins',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-skins.component.html',
  styleUrl: './admin-skins.component.scss'
})
export class AdminSkinsComponent implements OnInit {
  private adminSkinsService = inject(AdminSkinsService);

  public discs$!: Observable<DiscCustomization[]>;
  public currentPage = 0;
  public totalPages = 0;
  public pageSize = 10;

  public showCreateModal: WritableSignal<boolean> = signal(false);
  public showEditModal: WritableSignal<boolean> = signal(false);
  public showDeleteConfirm: WritableSignal<boolean> = signal(false);
  public editingDisc: WritableSignal<DiscCustomization | null> = signal(null);
  public discToDelete: WritableSignal<DiscCustomization | null> = signal(null);

  public formData = {
    itemCode: '',
    displayName: '',
    type: 'color' as 'color' | 'image',
    value: '',
    price: null as number | null,
    availableForPurchase: true
  };

  public previewValue: WritableSignal<string> = signal('');

  ngOnInit(): void {
    this.loadDiscs(this.currentPage);
  }

  loadDiscs(page: number): void {
    this.currentPage = page;
    this.discs$ = this.adminSkinsService.getAllDiscs(page, this.pageSize).pipe(
      map((response: ApiDiscsResponse) => {
        this.totalPages = response.totalPages;
        return response.content;
      })
    );
  }

  openCreateModal(): void {
    this.resetForm();
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.resetForm();
  }

  openEditModal(disc: DiscCustomization): void {
    this.editingDisc.set(disc);
    this.formData = {
      itemCode: disc.itemCode,
      displayName: disc.displayName,
      type: disc.type,
      value: disc.value,
      price: disc.price,
      availableForPurchase: disc.availableForPurchase ?? true
    };
    this.updatePreview();
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingDisc.set(null);
    this.resetForm();
  }

  createDisc(): void {
    const request: CreateDiscRequest = {
      itemCode: this.formData.itemCode,
      displayName: this.formData.displayName,
      type: this.formData.type,
      value: this.formData.value,
      price: this.formData.price,
      availableForPurchase: this.formData.availableForPurchase
    };

    this.adminSkinsService.createDisc(request).subscribe({
      next: () => {
        this.loadDiscs(this.currentPage);
        this.closeCreateModal();
      },
      error: (err) => console.error('Error creating disc:', err)
    });
  }

  updateDisc(): void {
    const disc = this.editingDisc();
    if (!disc) return;

    const request: CreateDiscRequest = {
      itemCode: this.formData.itemCode,
      displayName: this.formData.displayName,
      type: this.formData.type,
      value: this.formData.value,
      price: this.formData.price,
      availableForPurchase: this.formData.availableForPurchase
    };

    this.adminSkinsService.updateDisc(disc.itemCode, request).subscribe({
      next: () => {
        this.loadDiscs(this.currentPage);
        this.closeEditModal();
      },
      error: (err) => console.error('Error updating disc:', err)
    });
  }

  openDeleteConfirm(disc: DiscCustomization): void {
    this.discToDelete.set(disc);
    this.showDeleteConfirm.set(true);
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirm.set(false);
    this.discToDelete.set(null);
  }

  confirmDelete(): void {
    const disc = this.discToDelete();
    if (!disc) return;

    this.adminSkinsService.deleteDisc(disc.itemCode).subscribe({
      next: () => {
        this.loadDiscs(this.currentPage);
        this.closeDeleteConfirm();
      },
      error: (err) => console.error('Error deleting disc:', err)
    });
  }

  updatePreview(): void {
    this.previewValue.set(this.formData.value);
  }

  resetForm(): void {
    this.formData = {
      itemCode: '',
      displayName: '',
      type: 'color',
      value: '',
      price: null,
      availableForPurchase: true
    };
    this.previewValue.set('');
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.loadDiscs(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.loadDiscs(this.currentPage + 1);
    }
  }
}
