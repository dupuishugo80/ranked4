import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ProfileService } from './profile.service';
import { DiscCustomization, GameHistoryItem, UserProfile } from './profile.model';
import { LoginService } from '../security/login/login.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private readonly AI_PLAYER_UUID = '00000000-0000-0000-0000-000000000001';

  private profileService = inject(ProfileService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private loginService = inject(LoginService);

  public profile: WritableSignal<UserProfile | null> = signal(null);
  public showAvatarModal: WritableSignal<boolean> = signal(false);
  public newAvatarUrl: WritableSignal<string> = signal('');
  public isUpdatingAvatar: WritableSignal<boolean> = signal(false);
  public selectedDisc: WritableSignal<DiscCustomization | null> = signal(null);
  public isEquippingDisc: WritableSignal<boolean> = signal(false);
  public successMessage: WritableSignal<string> = signal('');
  public errorMessage: WritableSignal<string> = signal('');

  public userId: WritableSignal<string | null> = signal(null);
  public isOwnProfile = computed(() => {
    const paramUserId = this.userId();
    const currentUserId = this.loginService.getUserId();
    return !paramUserId || paramUserId === currentUserId;
  });

  public gameHistory: WritableSignal<GameHistoryItem[]> = signal([]);
  public historyCurrentPage: WritableSignal<number> = signal(0);
  public historyTotalPages: WritableSignal<number> = signal(0);
  public historyPageSize = 10;
  public isLoadingHistory: WritableSignal<boolean> = signal(false);

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const userIdParam = params.get('userId');
      this.userId.set(userIdParam);
      this.loadProfile();
      this.loadGameHistory(0);
    });
  }

  private loadProfile(): void {
    const userId = this.userId();
    const observable = userId
      ? this.profileService.getProfileById(userId)
      : this.profileService.getProfile();

    observable.subscribe({
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
        this.successMessage.set('Avatar updated successfully');
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

  private loadGameHistory(page: number): void {
    const userId = this.userId() || this.loginService.getUserId();
    if (!userId) return;

    this.isLoadingHistory.set(true);
    this.profileService.getGameHistory(userId, page, this.historyPageSize).subscribe({
      next: (response) => {
        this.gameHistory.set(response.content);
        this.historyTotalPages.set(response.totalPages);
        this.historyCurrentPage.set(page);
        this.isLoadingHistory.set(false);
      },
      error: (err) => {
        console.error('Error loading game history:', err);
        this.isLoadingHistory.set(false);
      }
    });
  }

  historyNextPage(): void {
    if (this.historyCurrentPage() < this.historyTotalPages() - 1) {
      this.loadGameHistory(this.historyCurrentPage() + 1);
    }
  }

  historyPreviousPage(): void {
    if (this.historyCurrentPage() > 0) {
      this.loadGameHistory(this.historyCurrentPage() - 1);
    }
  }

  getOpponentInfo(historyItem: GameHistoryItem): { name: string; id: string; isAI: boolean } {
    const currentProfileId = this.profile()?.userId;
    let opponentId: string;
    let opponentName: string;

    if (historyItem.playerOneId === currentProfileId) {
      opponentId = historyItem.playerTwoId;
      opponentName = historyItem.playerTwoName;
    } else {
      opponentId = historyItem.playerOneId;
      opponentName = historyItem.playerOneName;
    }

    const isAI = opponentId === this.AI_PLAYER_UUID;

    if (isAI && historyItem.aiDifficulty !== null) {
      const difficultyLabels: { [key: number]: string } = {
        1: 'EASY',
        2: 'MEDIUM',
        3: 'HARD'
      };
      opponentName = `IA - ${difficultyLabels[historyItem.aiDifficulty] || 'LEVEL ' + historyItem.aiDifficulty}`;
    }

    return {
      name: opponentName,
      id: opponentId,
      isAI: isAI
    };
  }

  getGameResult(historyItem: GameHistoryItem): { text: string; class: string } {
    const currentProfileId = this.profile()?.userId;

    if (!historyItem.winner) {
      return { text: 'Draw', class: 'draw' };
    }

    const isPlayerOne = historyItem.playerOneId === currentProfileId;
    const playerWon = (isPlayerOne && historyItem.winner === 'PLAYER_ONE') ||
                      (!isPlayerOne && historyItem.winner === 'PLAYER_TWO');

    return playerWon
      ? { text: 'Victory', class: 'win' }
      : { text: 'Defeat', class: 'loss' };
  }

  navigateToProfile(userId: string): void {
    this.router.navigate(['/profile', userId]);
  }
}