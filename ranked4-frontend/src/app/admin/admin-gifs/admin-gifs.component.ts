import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminGifsService } from './admin-gifs.service';
import { CreateGifRequest, Gif, UpdateGifRequest } from './admin-gifs.model';

@Component({
  selector: 'app-admin-gifs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-gifs.component.html',
  styleUrl: './admin-gifs.component.scss'
})
export class AdminGifsComponent implements OnInit {
  private adminGifsService = inject(AdminGifsService);

  public gifs: WritableSignal<Gif[]> = signal([]);
  public loading: WritableSignal<boolean> = signal(false);
  public errorMessage: WritableSignal<string> = signal('');
  public successMessage: WritableSignal<string> = signal('');

  public showCreateModal: WritableSignal<boolean> = signal(false);
  public showEditModal: WritableSignal<boolean> = signal(false);
  public showDeleteConfirm: WritableSignal<boolean> = signal(false);

  public editingGif: WritableSignal<Gif | null> = signal(null);
  public gifToDelete: WritableSignal<Gif | null> = signal(null);

  public newGif: WritableSignal<CreateGifRequest> = signal({
    code: '',
    assetPath: '',
    active: true
  });

  public editGifData: WritableSignal<UpdateGifRequest> = signal({});

  ngOnInit(): void {
    this.loadGifs();
  }

  loadGifs(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.adminGifsService.getAllGifs().subscribe({
      next: (gifs) => {
        this.gifs.set(gifs);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading GIFs:', err);
        this.errorMessage.set('Error loading GIFs');
        this.loading.set(false);
      }
    });
  }

  openCreateModal(): void {
    this.newGif.set({
      code: '',
      assetPath: '',
      active: true
    });
    this.showCreateModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  createGif(): void {
    const gif = this.newGif();
    if (!gif.code.trim() || !gif.assetPath.trim()) {
      this.errorMessage.set('Code and URL are required');
      return;
    }

    this.loading.set(true);
    this.adminGifsService.createGif(gif).subscribe({
      next: () => {
        this.successMessage.set('GIF created successfully');
        this.closeCreateModal();
        this.loadGifs();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => {
        console.error('Error creating GIF:', err);
        this.errorMessage.set(err.error?.message || 'Error creating GIF');
        this.loading.set(false);
      }
    });
  }

  openEditModal(gif: Gif): void {
    this.editingGif.set({ ...gif });
    this.editGifData.set({
      code: gif.code,
      assetPath: gif.assetPath,
      active: gif.active
    });
    this.showEditModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingGif.set(null);
  }

  updateGif(): void {
    const gif = this.editingGif();
    const data = this.editGifData();
    if (!gif) return;

    this.loading.set(true);
    this.adminGifsService.updateGif(gif.id, data).subscribe({
      next: () => {
        this.successMessage.set('GIF updated successfully');
        this.closeEditModal();
        this.loadGifs();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => {
        console.error('Error updating GIF:', err);
        this.errorMessage.set(err.error?.message || 'Error updating GIF');
        this.loading.set(false);
      }
    });
  }

  openDeleteConfirm(gif: Gif): void {
    this.gifToDelete.set(gif);
    this.showDeleteConfirm.set(true);
    this.errorMessage.set('');
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirm.set(false);
    this.gifToDelete.set(null);
  }

  confirmDelete(): void {
    const gif = this.gifToDelete();
    if (!gif) return;

    this.loading.set(true);
    this.adminGifsService.deleteGif(gif.id).subscribe({
      next: () => {
        this.successMessage.set('GIF deleted successfully');
        this.closeDeleteConfirm();
        this.loadGifs();
        setTimeout(() => this.successMessage.set(''), 3000);
      },
      error: (err) => {
        console.error('Error deleting GIF:', err);
        this.errorMessage.set(err.error?.message || 'Error deleting GIF');
        this.loading.set(false);
      }
    });
  }

  updateNewGifField(field: keyof CreateGifRequest, value: any): void {
    this.newGif.update(gif => ({ ...gif, [field]: value }));
  }

  updateEditGifField(field: keyof UpdateGifRequest, value: any): void {
    this.editGifData.update(data => ({ ...data, [field]: value }));
  }
}
