import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Observable, of } from 'rxjs';
import { GameHistoryEntry } from './game-history.model';
import { GameHistoryService } from './game-history.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-game-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-history.component.html',
  styleUrl: './game-history.component.scss'
})
export class GameHistoryComponent implements OnInit, OnChanges {
  private gameHistoryService = inject(GameHistoryService);
  private router = inject(Router);

  @Input() mode: 'global' | 'player' = 'global';
  @Input() userId: string | null = null;
  @Input() currentUserId: string | null = null;
  @Input() reloadTrigger: Observable<void> = new Observable<void>();

  public history$: Observable<GameHistoryEntry[]> | null = null;

  historyTrack = (index: number, entry: GameHistoryEntry) => entry.gameId;

  ngOnInit(): void {
    this.fetchHistory();

    this.reloadTrigger.subscribe(() => {
      this.fetchHistory();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['mode'] || changes['userId']) && !changes['userId']?.firstChange) {
      this.fetchHistory();
    }
  }

  fetchHistory(): void {
    if (this.mode === 'player') {
      if (this.userId) {
        this.history$ = this.gameHistoryService.getPlayerHistory(this.userId);
      } else {
        console.error('GameHistoryComponent: "userId" est requis pour le mode "player".');
        this.history$ = of([]);
      }
    } else {
      this.history$ = this.gameHistoryService.getGlobalHistory();
    }
  }

  isCurrentUser(game: GameHistoryEntry): boolean {
    if (!this.currentUserId) return false;
    return game.playerOneId === this.currentUserId || game.playerTwoId === this.currentUserId;
  }

  navigateToProfile(userId: string): void {
    if (!userId) {
      console.error('userId is undefined');
      return;
    }

    if (userId === this.currentUserId) {
      this.router.navigate(['/profile']);
    } else {
      this.router.navigate(['/profile', userId]);
    }
  }
}
