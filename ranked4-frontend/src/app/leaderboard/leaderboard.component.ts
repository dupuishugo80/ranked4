import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, Input } from '@angular/core';
import { Observable } from 'rxjs';
import { LeaderboardService } from './leaderboard.service';
import { UserProfile } from '../profile/profile.model';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './leaderboard.component.html',
  styleUrl: './leaderboard.component.scss'
})
export class LeaderboardComponent implements OnInit {
  private leaderboardService = inject(LeaderboardService);

  public leaderboard$: Observable<UserProfile[]> | null = null;

  @Input() currentUserId: string | null = null;
  @Input() reloadTrigger: Observable<void> = new Observable<void>();

  playerTrack = (index: number, player: UserProfile) => player.userId ?? index;

  ngOnInit(): void {
    this.fetchLeaderboard();

    this.reloadTrigger.subscribe(() => {
      this.fetchLeaderboard();
    });
  }

  fetchLeaderboard(): void {
    this.leaderboard$ = this.leaderboardService.getLeaderboard();
  }

  getWinRate(player: UserProfile): number {
    const totalGames = player.wins + player.losses;
    if (totalGames === 0) {
      return 0;
    }
    return Math.round((player.wins / totalGames) * 100);
  }
}
