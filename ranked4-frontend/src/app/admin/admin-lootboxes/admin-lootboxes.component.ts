import { CommonModule } from "@angular/common";
import { Component, OnInit, inject, signal, WritableSignal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { map, Observable } from "rxjs";
import { DiscCustomization } from "../admin-skin/admin-skins.model";
import { AdminSkinsService } from "../admin-skin/admin-skins.service";
import { ApiLootboxesResponse, CreateLootboxRequest, Lootbox, LootboxContent, LootboxDetail } from "./admin-lootboxes.model";
import { AdminLootboxesService } from "./admin-lootboxes.service";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-lootboxes.component.html',
  styleUrl: './admin-lootboxes.component.scss'
})
export class AdminLootboxesComponent implements OnInit {

  private adminLootboxesService = inject(AdminLootboxesService);
  private adminSkinsService = inject(AdminSkinsService);

  public lootboxes$!: Observable<Lootbox[]>;
  public availableDiscs$!: Observable<DiscCustomization[]>;
  public currentPage = 0;
  public totalPages = 0;
  public pageSize = 10;

  public showCreateModal: WritableSignal<boolean> = signal(false);
  public showEditModal: WritableSignal<boolean> = signal(false);
  public showDeleteConfirm: WritableSignal<boolean> = signal(false);
  public editingLootbox: WritableSignal<LootboxDetail | null> = signal(null);
  public lootboxToDelete: WritableSignal<Lootbox | null> = signal(null);

  public formData = {
    name: '',
    description: '',
    imageUrl: '',
    price: 0,
    dailyFree: false,
    contents: [] as LootboxContent[]
  };

  ngOnInit(): void {
    this.loadLootboxes(this.currentPage);
    this.loadAvailableDiscs();
  }

  private loadLootboxes(page: number): void {
    this.lootboxes$ = this.adminLootboxesService.getLootboxes(page, this.pageSize).pipe(
      map((response: ApiLootboxesResponse) => {
        this.totalPages = response.totalPages;
        this.currentPage = response.number;
        return response.content;
      })
    );
  }

  private loadAvailableDiscs(): void {
    this.availableDiscs$ = this.adminSkinsService.getAllDiscs(0, 100).pipe(
      map(response => response.content)
    );
  }

  openCreateModal(): void {
    this.formData = {
      name: '',
      description: '',
      imageUrl: '',
      price: 0,
      dailyFree: false,
      contents: []
    };
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  openEditModal(lootbox: Lootbox): void {
    this.adminLootboxesService.getLootboxById(lootbox.id).subscribe({
      next: (detail: LootboxDetail) => {
        this.editingLootbox.set(detail);
        this.formData = {
          name: detail.name,
          description: detail.description,
          imageUrl: detail.imageUrl,
          price: detail.price,
          dailyFree: detail.dailyFree || false,
          contents: [...detail.contents]
        };
        this.showEditModal.set(true);
      },
      error: (err) => console.error('Error loading lootbox details:', err)
    });
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.editingLootbox.set(null);
  }

  openDeleteConfirm(lootbox: Lootbox): void {
    this.lootboxToDelete.set(lootbox);
    this.showDeleteConfirm.set(true);
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirm.set(false);
    this.lootboxToDelete.set(null);
  }

  createLootbox(): void {
    const request: CreateLootboxRequest = {
      name: this.formData.name,
      description: this.formData.description,
      imageUrl: this.formData.imageUrl,
      price: this.formData.price,
      dailyFree: this.formData.dailyFree,
      contents: this.formData.contents.map(c => ({
        itemCode: c.itemCode,
        itemType: c.itemType,
        weight: c.weight,
        goldAmount: c.goldAmount
      }))
    };

    this.adminLootboxesService.createLootbox(request).subscribe({
      next: () => {
        this.loadLootboxes(this.currentPage);
        this.closeCreateModal();
      },
      error: (err) => console.error('Error creating lootbox:', err)
    });
  }

  updateLootbox(): void {
    const lootbox = this.editingLootbox();
    if (!lootbox) return;

    const request: CreateLootboxRequest = {
      name: this.formData.name,
      description: this.formData.description,
      imageUrl: this.formData.imageUrl,
      price: this.formData.price,
      dailyFree: this.formData.dailyFree,
      contents: this.formData.contents.map(c => ({
        itemCode: c.itemCode,
        itemType: c.itemType,
        weight: c.weight,
        goldAmount: c.goldAmount
      }))
    };

    this.adminLootboxesService.updateLootbox(lootbox.id, request).subscribe({
      next: () => {
        this.loadLootboxes(this.currentPage);
        this.closeEditModal();
      },
      error: (err) => console.error('Error updating lootbox:', err)
    });
  }

  confirmDelete(): void {
    const lootbox = this.lootboxToDelete();
    if (!lootbox) return;

    this.adminLootboxesService.deleteLootbox(lootbox.id).subscribe({
      next: () => {
        this.loadLootboxes(this.currentPage);
        this.closeDeleteConfirm();
      },
      error: (err) => console.error('Error deleting lootbox:', err)
    });
  }

  addContentRow(): void {
    this.formData.contents.push({
      itemCode: '',
      itemType: 'DISC',
      weight: 1,
      goldAmount: undefined
    });
  }

  removeContentRow(index: number): void {
    this.formData.contents.splice(index, 1);
  }

  getTotalWeight(): number {
    return this.formData.contents.reduce((sum, content) => sum + content.weight, 0);
  }

  calculateProbability(weight: number): number {
    const total = this.getTotalWeight();
    return total > 0 ? (weight / total) * 100 : 0;
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.loadLootboxes(this.currentPage - 1);
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.loadLootboxes(this.currentPage + 1);
    }
  }
}