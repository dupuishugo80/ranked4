import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnInit, Output, signal, WritableSignal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Lootbox, LootboxContent } from '../shop.model';
import { API_ENDPOINTS } from '../../core/config/api.config';

interface LootboxItem {
  itemCode: string;
  itemType: 'DISC' | 'GOLD';
  goldAmount?: number;
  displayName: string;
  imageUrl?: string;
  skinType?: 'color' | 'image';
  value?: string;
  customMessage?: string;
  actualRewardType?: 'DISC' | 'GOLD';
}

interface DiscInfo {
  itemCode: string;
  displayName: string;
  type: 'color' | 'image';
  value: string;
}

@Component({
  selector: 'app-lootbox-opening',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './lootbox-opening.component.html',
  styleUrl: './lootbox-opening.component.scss'
})
export class LootboxOpeningComponent implements OnInit {
  private http = inject(HttpClient);
  private audioContext: AudioContext | null = null;

  @Input() lootbox!: Lootbox;
  @Input() userGold: number = 0;
  @Output() close = new EventEmitter<void>();
  @Output() open = new EventEmitter<void>();

  public showAnimation: WritableSignal<boolean> = signal(false);
  public isOpening: WritableSignal<boolean> = signal(false);
  public reel: WritableSignal<LootboxItem[]> = signal([]);
  public wonItem: WritableSignal<LootboxItem | null> = signal(null);
  public scrollPosition: WritableSignal<number> = signal(0);
  public discsMap: WritableSignal<Map<string, DiscInfo>> = signal(new Map());

  ngOnInit(): void {
    this.initAudioContext();
    this.loadDiscsInfo();
  }

  private initAudioContext(): void {
    // Initialiser l'AudioContext une seule fois pour de meilleures performances
    this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
  }

  private loadDiscsInfo(): void {
    this.http.get<any>(`${API_ENDPOINTS.DISCS}?page=0&size=1000`).subscribe({
      next: (response) => {
        const discs = response.content || response;
        console.log('Loaded discs for lootbox:', discs);
        const map = new Map<string, DiscInfo>();
        discs.forEach((disc: any) => {
          map.set(disc.itemCode, {
            itemCode: disc.itemCode,
            displayName: disc.displayName,
            type: disc.type,
            value: disc.value
          });
        });
        this.discsMap.set(map);
        console.log('DiscsMap size:', this.discsMap().size);
        console.log('Lootbox contents:', this.lootbox.contents);
        this.generateReel();
      },
      error: (err) => {
        console.error('Error loading discs:', err);
        this.generateReel();
      }
    });
  }

  getDiscInfo(itemCode: string): DiscInfo | undefined {
    return this.discsMap().get(itemCode);
  }

  private generateReel(): void {
    if (!this.lootbox.contents || this.lootbox.contents.length === 0) {
      console.error('Lootbox has no contents');
      return;
    }

    const items: LootboxItem[] = [];
    const totalWeight = this.lootbox.contents.reduce((sum, c) => sum + c.weight, 0);

    this.lootbox.contents.forEach(content => {
      const count = Math.round((content.weight / totalWeight) * 100);
      const discInfo = content.itemType === 'DISC' ? this.getDiscInfo(content.itemCode) : undefined;

      for (let i = 0; i < count; i++) {
        items.push({
          itemCode: content.itemCode || '',
          itemType: content.itemType,
          goldAmount: content.goldAmount,
          displayName: content.itemType === 'GOLD'
            ? `${content.goldAmount} Gold`
            : (discInfo?.displayName || content.itemCode),
          skinType: discInfo?.type,
          value: discInfo?.value
        });
      }
    });

    const shuffled = items.sort(() => Math.random() - 0.5);

    const repeatedReel: LootboxItem[] = [];
    const repeatCount = 5;
    for (let i = 0; i < repeatCount; i++) {
      repeatedReel.push(...shuffled);
    }

    this.reel.set(repeatedReel);
  }

  canAfford(): boolean {
    return this.userGold >= this.lootbox.price;
  }

  getProbability(content: LootboxContent): number {
    const totalWeight = this.lootbox.contents?.reduce((sum, c) => sum + c.weight, 0) || 1;
    return (content.weight / totalWeight) * 100;
  }

  startOpening(): void {
    if (!this.canAfford() || this.isOpening()) return;

    if (this.audioContext?.state === 'suspended') {
      this.audioContext.resume();
    }

    this.showAnimation.set(true);
    this.isOpening.set(true);
    this.open.emit();
  }

  public animateToResult(result: LootboxItem): void {
    if (result.itemType === 'DISC' && result.itemCode) {
      const discInfo = this.getDiscInfo(result.itemCode);
      if (discInfo) {
        result.skinType = discInfo.type;
        result.value = discInfo.value;
        result.displayName = discInfo.displayName;
      }
    }

    this.wonItem.set(result);

    const resultIndex = this.reel().findIndex(item => {
      if (result.itemCode && result.itemCode.trim()) {
        return item.itemCode === result.itemCode;
      }

      if (item.itemType !== result.itemType) return false;

      if (item.itemType === 'GOLD') {
        return item.goldAmount === result.goldAmount;
      } else {
        return item.itemCode === result.itemCode;
      }
    });

    if (resultIndex === -1) {
      console.error('Result not found in reel', {
        result,
        reelSample: this.reel().slice(0, 5)
      });
      return;
    }

    const itemWidth = 120;
    const containerWidth = 600;
    const centerOffset = containerWidth / 2 - itemWidth / 2;
    const baseReelLength = Math.floor(this.reel().length / 5);
    const targetRepetition = 2;
    const adjustedIndex = resultIndex + (targetRepetition * baseReelLength);

    const reelPadding = 10;
    const targetPosition = -(reelPadding + adjustedIndex * itemWidth - centerOffset);

    const fastDuration = 4000;
    const slowDuration = 4000 + Math.random() * 1500;

    this.animateScrollTwoPhase(targetPosition, fastDuration, slowDuration);
  }

  private playTickSound(volume: number = 0.3): void {
    if (!this.audioContext) {
      this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
    }

    if (this.audioContext.state === 'suspended') {
      return;
    }

    try {
      const oscillator = this.audioContext.createOscillator();
      const gainNode = this.audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(this.audioContext.destination);

      oscillator.frequency.value = 800;
      oscillator.type = 'sine';

      const clampedVolume = Math.max(0.05, Math.min(volume, 0.3));
      gainNode.gain.setValueAtTime(clampedVolume, this.audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.05);

      oscillator.start(this.audioContext.currentTime);
      oscillator.stop(this.audioContext.currentTime + 0.05);
    } catch (error) {
      console.warn('Error playing tick sound:', error);
    }
  }

  private animateScrollTwoPhase(targetPosition: number, fastDuration: number, slowDuration: number): void {
    const startPosition = 0;
    const startTime = Date.now();
    const itemWidth = 120;
    const totalDuration = fastDuration + slowDuration;

    let totalItemsPassed = 0;
    let lastPosition = startPosition;

    const animate = () => {
      const elapsed = Date.now() - startTime;

      if (elapsed >= totalDuration) {
        this.scrollPosition.set(targetPosition);
        setTimeout(() => {
          this.isOpening.set(false);
        }, 1000);
        return;
      }

      const progress = elapsed / totalDuration;
      const easedProgress = 1 - Math.pow(1 - progress, 3);

      const currentPosition = startPosition + (targetPosition - startPosition) * easedProgress;
      const newItemsPassed = Math.floor(Math.abs(currentPosition) / itemWidth);

      if (newItemsPassed > totalItemsPassed) {
        const itemsToPlay = newItemsPassed - totalItemsPassed;

        for (let i = 0; i < itemsToPlay; i++) {
          const volume = 0.3 * (1 - progress * 0.6);

          if (itemsToPlay > 3) {
            if (i % Math.ceil(itemsToPlay / 3) !== 0) continue;
          }

          this.playTickSound(volume);
        }

        totalItemsPassed = newItemsPassed;
      }

      lastPosition = currentPosition;
      this.scrollPosition.set(currentPosition);
      requestAnimationFrame(animate);
    };

    requestAnimationFrame(animate);
  }

  closeModal(): void {
    if (this.isOpening()) return;
    this.close.emit();
  }

  resetAnimation(): void {
    this.showAnimation.set(false);
    this.wonItem.set(null);
    this.scrollPosition.set(0);
    this.isOpening.set(false);
  }

  ngOnDestroy(): void {
    if (this.audioContext) {
      this.audioContext.close();
      this.audioContext = null;
    }
  }
}