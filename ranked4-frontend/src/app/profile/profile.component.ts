import { CommonModule } from '@angular/common';
import { Component, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProfileService } from './profile.service';
import { DiscCustomization, UserProfile } from './profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private profileService = inject(ProfileService);

  public profile: WritableSignal<UserProfile | null> = signal(null);
  public showAvatarModal: WritableSignal<boolean> = signal(false);
  public newAvatarUrl: WritableSignal<string> = signal('');
  public isUpdatingAvatar: WritableSignal<boolean> = signal(false);
  public selectedDisc: WritableSignal<DiscCustomization | null> = signal(null);
  public isEquippingDisc: WritableSignal<boolean> = signal(false);
  public successMessage: WritableSignal<string> = signal('');
  public errorMessage: WritableSignal<string> = signal('');

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
      },
      error: (err) => {
        console.error('Error loading profile:', err);
        this.errorMessage.set('Error loading profile');
      }
    });
  }

  openAvatarModal(): void {
    this.newAvatarUrl.set(this.profile()?.avatarUrl || '');
    this.showAvatarModal.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  closeAvatarModal(): void {
    this.showAvatarModal.set(false);
    this.newAvatarUrl.set('');
  }

  updateAvatar(): void {
    const url = this.newAvatarUrl();
    if (!url.trim()) {
      this.errorMessage.set('Please enter a valid URL');
      return;
    }

    this.isUpdatingAvatar.set(true);
    this.errorMessage.set('');

    this.profileService.updateAvatar(url).subscribe({
      next: (updatedProfile) => {
        this.profile.set(updatedProfile);
        this.successMessage.set('Avatar updated successfully !');
        this.isUpdatingAvatar.set(false);
        setTimeout(() => {
          this.closeAvatarModal();
          this.successMessage.set('');
        }, 1500);
      },
      error: (err) => {
        console.error('Error updating avatar:', err);
        this.errorMessage.set(err.error?.message || 'Error updating avatar');
        this.isUpdatingAvatar.set(false);
      }
    });
  }

  selectDisc(disc: DiscCustomization): void {
    if (this.isEquippingDisc()) return;

    const currentEquipped = this.profile()?.equippedDisc;
    if (currentEquipped?.itemCode === disc.itemCode) {
      return;
    }

    this.selectedDisc.set(disc);
    this.equipSelectedDisc();
  }

  selectNoDisc(): void {
    if (this.isEquippingDisc()) return;

    const currentEquipped = this.profile()?.equippedDisc;
    if (!currentEquipped) {
      return;
    }

    this.unequipDisc();
  }

  private equipSelectedDisc(): void {
    const disc = this.selectedDisc();
    if (!disc || !disc.itemCode) return;

    this.isEquippingDisc.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.profileService.equipDisc(disc.itemCode).subscribe({
      next: () => {
        this.successMessage.set(`${disc.displayName || disc.itemCode} equipped successfully !`);
        this.isEquippingDisc.set(false);
        this.loadProfile();
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Error equipping disc');
        this.isEquippingDisc.set(false);
      }
    });
  }

  private unequipDisc(): void {
    this.isEquippingDisc.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    this.profileService.unequipDisc().subscribe({
      next: () => {
        this.successMessage.set('Default disc unequipped successfully !');
        this.isEquippingDisc.set(false);
        this.loadProfile();
        setTimeout(() => {
          this.successMessage.set('');
        }, 3000);
      },
      error: (err) => {
        this.errorMessage.set(err.error?.message || 'Error unequipping disc');
        this.isEquippingDisc.set(false);
      }
    });
  }

  isDiscEquipped(disc: DiscCustomization): boolean {
    const equipped = this.profile()?.equippedDisc;
    return equipped?.itemCode === disc.itemCode;
  }

  isNoDiscEquipped(): boolean {
    return !this.profile()?.equippedDisc;
  }

  getSortedDiscs(): DiscCustomization[] {
    const p = this.profile();
    if (!p || !p.ownedDiscs) return [];

    const discs = [...p.ownedDiscs];
    const equippedItemCode = p.equippedDisc?.itemCode;

    if (!equippedItemCode) return discs;

    return discs.sort((a, b) => {
      const aIsEquipped = a.itemCode === equippedItemCode;
      const bIsEquipped = b.itemCode === equippedItemCode;

      if (aIsEquipped && !bIsEquipped) return -1;
      if (!aIsEquipped && bIsEquipped) return 1;
      return 0;
    });
  }

  getWinRate(): number {
    const p = this.profile();
    if (!p || p.gamesPlayed === 0) return 0;
    return Math.round((p.wins / p.gamesPlayed) * 100);
  }
}
