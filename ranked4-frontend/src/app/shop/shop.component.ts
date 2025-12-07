import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, ViewChild, WritableSignal } from '@angular/core';
import { Router } from '@angular/router';
import { ProfileService } from '../profile/profile.service';
import { Lootbox, Product, Skin } from './shop.model';
import { ShopService } from './shop.service';
import { LootboxOpeningComponent } from './lootbox-opening/lootbox-opening.component';

@Component({
  selector: 'app-shop',
  standalone: true,
  imports: [CommonModule, LootboxOpeningComponent],
  templateUrl: './shop.component.html',
  styleUrl: './shop.component.scss'
})
export class ShopComponent implements OnInit {
  private shopService = inject(ShopService);
  private profileService = inject(ProfileService);
  private router = inject(Router);

  @ViewChild(LootboxOpeningComponent) lootboxOpeningComponent?: LootboxOpeningComponent;

  public products: WritableSignal<Product[]> = signal([]);
  public lootboxes: WritableSignal<Lootbox[]> = signal([]);
  public userGold: WritableSignal<number> = signal(0);
  public ownedItemCodes: WritableSignal<Set<string>> = signal(new Set());
  public activeTab: WritableSignal<'skins' | 'lootboxes'> = signal('skins');
  public showPurchaseModal: WritableSignal<boolean> = signal(false);
  public selectedProduct: WritableSignal<Product | null> = signal(null);
  public purchaseMessage: WritableSignal<string> = signal('');
  public isPurchasing: WritableSignal<boolean> = signal(false);
  public showLootboxOpening: WritableSignal<boolean> = signal(false);
  public selectedLootbox: WritableSignal<Lootbox | null> = signal(null);

  ngOnInit(): void {
    this.loadUserProfile();
    this.loadProducts();
    this.loadLootboxes();
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

  private loadProducts(): void {
    this.shopService.getProducts().subscribe({
      next: (products) => {
        console.log('Loaded products:', products);
        this.products.set(products);
      },
      error: (err) => console.error('Error loading products:', err)
    });
  }

  private loadLootboxes(): void {
    this.shopService.getLootboxes().subscribe({
      next: (lootboxes) => {
        console.log('Loaded lootboxes:', lootboxes);
        this.lootboxes.set(lootboxes);
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
}
