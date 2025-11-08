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
  styleUrls: ['./matchmaking.component.scss']
})
export class MatchmakingComponent implements OnInit, OnDestroy {
  public gameService = inject(GameService);
  private router = inject(Router);
  private statusSub: Subscription | null = null;

  status: string = 'Connecting...';

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

    if (this.gameService.gameStatus$.value === 'IDLE') {
      this.status = 'Connecting to matchmaking service...';
      this.gameService.joinQueue();
    } else if (this.gameService.gameStatus$.value === 'QUEUEING') {
      this.status = 'Already in queue. Searching...';
    }
  }

  ngOnDestroy(): void {
    this.statusSub?.unsubscribe();
    if (this.gameService.gameStatus$.value === 'QUEUEING') {
      console.log("Annulation du matchmaking (l'utilisateur a quitté la page)");
      this.gameService.leaveGame();
    }
  }

  cancelMatchmaking(): void {
    this.gameService.leaveGame();
  }
}