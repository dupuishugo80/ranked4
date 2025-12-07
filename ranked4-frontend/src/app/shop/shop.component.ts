import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, ViewChild, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProfileService } from '../profile/profile.service';
import { Lootbox, Product, Skin } from './shop.model';
import { PageResponse, ShopService } from './shop.service';
import { LootboxOpeningComponent } from './lootbox-opening/lootbox-opening.component';

@Component({
  selector: 'app-shop',
  standalone: true,
  imports: [CommonModule, FormsModule, LootboxOpeningComponent],
  templateUrl: './shop.component.html',
  styleUrl: './shop.component.scss'
})
export class ShopComponent implements OnInit {
  private shopService = inject(ShopService);
  private profileService = inject(ProfileService);
  private router = inject(Router);

  @ViewChild(LootboxOpeningComponent) lootboxOpeningComponent?: LootboxOpeningComponent;

  public products: WritableSignal<Product[]> = signal([]);
  public allProducts: Skin[] = [];
  public lootboxes: WritableSignal<Lootbox[]> = signal([]);
  public allLootboxes: Lootbox[] = [];
  public userGold: WritableSignal<number> = signal(0);
  public ownedItemCodes: WritableSignal<Set<string>> = signal(new Set());
  public activeTab: WritableSignal<'skins' | 'lootboxes'> = signal('skins');
  public showPurchaseModal: WritableSignal<boolean> = signal(false);
  public selectedProduct: WritableSignal<Product | null> = signal(null);
  public purchaseMessage: WritableSignal<string> = signal('');
  public isPurchasing: WritableSignal<boolean> = signal(false);
  public showLootboxOpening: WritableSignal<boolean> = signal(false);
  public selectedLootbox: WritableSignal<Lootbox | null> = signal(null);

  public skinsCurrentPage = 0;
  public skinsTotalPages = 0;
  public skinsPageSize = 9;

  public lootboxesCurrentPage = 0;
  public lootboxesTotalPages = 0;
  public lootboxesPageSize = 9;

  public selectedRarity: 'ALL' | 'COMMON' | 'UNCOMMON' | 'RARE' | 'EPIC' | 'LEGENDARY' = 'ALL';
  public priceSort: 'NONE' | 'ASC' | 'DESC' = 'NONE';
  public lootboxPriceSort: 'NONE' | 'ASC' | 'DESC' = 'NONE';

  ngOnInit(): void {
    this.loadUserProfile();
    this.loadProducts(this.skinsCurrentPage);
    this.loadLootboxes(this.lootboxesCurrentPage);
  }

  private loadUserProfile(): void {
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.userGold.set(profile.gold);
        if (profile.ownedDiscs) {
          this.ownedItemCodes.set(new Set(profile.ownedDiscs.map(d => d.itemCode).filter((code): code is string => !!code)));
        }
      },
      error: (err) => console.error('Error loading profile:', err)
    });
  }

  private loadProducts(page: number): void {
    this.skinsCurrentPage = page;
    this.shopService.getProducts(page, this.skinsPageSize).subscribe({
      next: (response: PageResponse<Skin>) => {
        console.log('Loaded products:', response);
        this.allProducts = response.content;
        this.skinsTotalPages = response.totalPages;
        this.applyFilters();
      },
      error: (err) => console.error('Error loading products:', err)
    });
  }

  private loadLootboxes(page: number): void {
    this.lootboxesCurrentPage = page;
    this.shopService.getLootboxes(page, this.lootboxesPageSize).subscribe({
      next: (response: PageResponse<any>) => {
        console.log('Loaded lootboxes:', response);
        this.allLootboxes = response.content;
        this.lootboxesTotalPages = response.totalPages;
        this.applyLootboxSort();
      },
      error: (err) => console.error('Error loading lootboxes:', err)
    });
  }

  switchTab(tab: 'skins' | 'lootboxes'): void {
    this.activeTab.set(tab);
  }

  openPurchaseModal(product: Product): void {
    if (this.isOwned(product)) {
      return;
    }
    this.selectedProduct.set(product);
    this.showPurchaseModal.set(true);
    this.purchaseMessage.set('');
  }

  closePurchaseModal(): void {
    this.showPurchaseModal.set(false);
    this.selectedProduct.set(null);
    this.purchaseMessage.set('');
  }

  confirmPurchase(): void {
    const product = this.selectedProduct();
    if (!product || this.isPurchasing()) return;

    if (this.userGold() < product.price) {
      this.purchaseMessage.set('Insufficient gold!');
      return;
    }

    this.isPurchasing.set(true);

    const itemCode = (product as Skin).itemCode;
    if (!itemCode) {
      this.purchaseMessage.set('Invalid product');
      this.isPurchasing.set(false);
      return;
    }

    this.shopService.purchaseProduct(itemCode).subscribe({
      next: (response) => {
        this.isPurchasing.set(false);
        if (response.success) {
          this.purchaseMessage.set('Purchase successful!');
          if (response.newBalance !== undefined) {
            this.userGold.set(response.newBalance);
          }
          this.loadUserProfile();
          setTimeout(() => {
            this.closePurchaseModal();
          }, 1500);
        } else {
          this.purchaseMessage.set(response.message || 'Purchase failed');
        }
      },
      error: (err) => {
        this.isPurchasing.set(false);
        this.purchaseMessage.set(err.error?.message || 'An error occurred');
      }
    });
  }

  canAfford(price: number): boolean {
    return this.userGold() >= price;
  }

  isOwned(product: Product): boolean {
    if (product.type === 'SKIN' && product.itemCode) {
      return this.ownedItemCodes().has(product.itemCode);
    }
    return false;
  }

  getSkins(): Skin[] {
    return this.products().filter(p => p.type === 'SKIN') as Skin[];
  }

  openLootboxModal(lootbox: Lootbox): void {
    this.selectedLootbox.set(lootbox);
    this.showLootboxOpening.set(true);
  }

  closeLootboxModal(): void {
    this.showLootboxOpening.set(false);
    this.selectedLootbox.set(null);
  }

  handleLootboxOpen(): void {
    const lootbox = this.selectedLootbox();
    if (!lootbox) return;

    if (this.userGold() < lootbox.price) {
      alert('Insufficient gold !');
      return;
    }

    this.shopService.openLootbox(lootbox.id).subscribe({
      next: (result) => {
        console.log('Lootbox opened:', result);

        this.userGold.update(gold => gold - lootbox.price);

        if (this.lootboxOpeningComponent) {
          const isDuplicate = result.displayMessage && result.displayMessage.includes('possédez déjà');

          this.lootboxOpeningComponent.animateToResult({
            itemCode: result.rewardItemCode || '',
            itemType: result.rewardItemType,
            goldAmount: result.rewardGoldAmount ?? undefined,
            displayName: result.rewardItemType === 'GOLD'
              ? `${result.rewardGoldAmount} Gold`
              : result.rewardItemCode,
            customMessage: result.displayMessage || undefined,
            actualRewardType: result.rewardItemType
          });
        }

        setTimeout(() => {
          this.loadUserProfile();
        }, 1000);
      },
      error: (err) => {
        console.error('Error opening lootbox:', err);
        alert('Error opening lootbox');
      }
    });
  }

  extractRarity(itemCode: string): 'COMMON' | 'UNCOMMON' | 'RARE' | 'EPIC' | 'LEGENDARY' {
    const upper = itemCode.toUpperCase();
    if (upper.includes('LEGENDARY')) return 'LEGENDARY';
    if (upper.includes('EPIC')) return 'EPIC';
    if (upper.includes('RARE')) return 'RARE';
    if (upper.includes('UNCOMMON')) return 'UNCOMMON';
    return 'COMMON';
  }

  applyFilters(): void {
    let filtered = [...this.allProducts];

    if (this.selectedRarity !== 'ALL') {
      filtered = filtered.filter(skin => this.extractRarity(skin.itemCode) === this.selectedRarity);
    }

    if (this.priceSort === 'ASC') {
      filtered.sort((a, b) => a.price - b.price);
    } else if (this.priceSort === 'DESC') {
      filtered.sort((a, b) => b.price - a.price);
    }

    this.products.set(filtered);
  }

  applyLootboxSort(): void {
    let sorted = [...this.allLootboxes];

    if (this.lootboxPriceSort === 'ASC') {
      sorted.sort((a, b) => a.price - b.price);
    } else if (this.lootboxPriceSort === 'DESC') {
      sorted.sort((a, b) => b.price - a.price);
    }

    this.lootboxes.set(sorted);
  }

  previousSkinsPage(): void {
    if (this.skinsCurrentPage > 0) {
      this.loadProducts(this.skinsCurrentPage - 1);
    }
  }

  nextSkinsPage(): void {
    if (this.skinsCurrentPage < this.skinsTotalPages - 1) {
      this.loadProducts(this.skinsCurrentPage + 1);
    }
  }

  previousLootboxesPage(): void {
    if (this.lootboxesCurrentPage > 0) {
      this.loadLootboxes(this.lootboxesCurrentPage - 1);
    }
  }

  nextLootboxesPage(): void {
    if (this.lootboxesCurrentPage < this.lootboxesTotalPages - 1) {
      this.loadLootboxes(this.lootboxesCurrentPage + 1);
    }
  }
}
