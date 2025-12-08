import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { Router } from '@angular/router';
import { GameService } from '../game/game.service';

@Component({
  selector: 'app-matchmaking',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './matchmaking.component.html',
  styleUrl: './matchmaking.component.scss'
})
export class MatchmakingComponent implements OnInit, OnDestroy {
  public gameService = inject(GameService);
  private router = inject(Router);
  private statusSub: Subscription | null = null;
  private timeSub: Subscription | null = null;

  status: string = 'Connecting...';
  public elapsedTime: string = '00:00';

  ngOnInit(): void {
    this.statusSub = this.gameService.gameStatus$.subscribe(status => {
      switch (status) {
        case 'QUEUEING':
          this.status = 'In queue. Searching for an opponent...';
          break;
        case 'IN_GAME':
          this.status = 'Opponent found! Starting...';
          break;
        case 'IDLE':
          this.status = 'Cancelling...';
          break;
      }
    });

    this.timeSub = this.gameService.queueTime$.subscribe(seconds => {
      this.elapsedTime = this.formatTime(seconds);
    });

    if (this.gameService.gameStatus$.value !== 'QUEUEING') {
      this.gameService.leaveGame();
      this.gameService.joinQueue();
    }
  }

  ngOnDestroy(): void {
    this.statusSub?.unsubscribe();
    this.timeSub?.unsubscribe();
    if (this.gameService.gameStatus$.value === 'QUEUEING') {
      this.gameService.leaveGame();
    }
  }

  private formatTime(totalSeconds: number): string {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${this.pad(minutes)}:${this.pad(seconds)}`;
  }

  private pad(num: number): string {
    return num < 10 ? '0' + num : '' + num;
  }

  cancelMatchmaking(): void {
    this.gameService.leaveGame();
    this.router.navigate(['/home']);
  }
}