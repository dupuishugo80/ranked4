import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { filter, Observable, Subscription } from 'rxjs';
import { GameUpdate, PlayerDisc } from './game.model';
import { GameService } from './game.service';
import { UserProfile } from '../../profile/profile.model';
import { ProfileService } from '../../profile/profile.service';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game.component.html',
  styleUrls: ['./game.component.scss']
})
export class GameComponent implements OnInit, OnDestroy {
  
  public gameService = inject(GameService);
  private router = inject(Router);
  private profileService = inject(ProfileService);

  public myProfile$: Observable<UserProfile> | null = null;
  public opponentProfile$: Observable<UserProfile> | null = null;

  public gameState$!: Observable<GameUpdate | null>;
  public gameError$!: Observable<string>;

  public board: string[][] = [];
  public myDisc: PlayerDisc | null = null;
  public isMyTurn: boolean = false;
  public gameStatus: 'IN_PROGRESS' | 'FINISHED' = 'IN_PROGRESS';
  public gameMessage: string = "Loading the game...";

  private stateSub: Subscription | null = null;
  private errorSub: Subscription | null = null;
  private opponentIdSet = false;
  
  public lastMove: { row: number, col: number } | null = null;

  public isLoser: boolean = false;

  ngOnInit(): void {
    this.gameState$ = this.gameService.gameState$;
    
    this.myDisc = this.gameService.getMyPlayerDisc();
    this.myProfile$ = this.profileService.getProfile();

    this.stateSub = this.gameState$.pipe(filter(state => state !== null)).subscribe(state => {
      if (state && !this.opponentIdSet) {
        const playerOneId = state.playerOneId;
        const playerTwoId = state.playerTwoId;

        const opponentId = (this.myDisc === 'PLAYER_ONE') ? playerTwoId : playerOneId;
          
        if (opponentId) {
          this.opponentProfile$ = this.profileService.getProfileById(opponentId);
          this.opponentIdSet = true;
        }
      }

      this.gameStatus = state.status;
      this.isMyTurn = state.nextPlayer === this.myDisc && state.status === 'IN_PROGRESS';
      
      if (this.board.flat().join('') !== state.boardState) {
        this.updateLastMove(state.boardState);
      }

      this.board = this.parseBoardState(state.boardState);
      this.updateGameMessage(state!);
    });
  }

  isOpponentP1(): boolean {
    return this.myDisc === 'PLAYER_TWO';
  }

  private parseBoardState(boardState: string): string[][] {
    const rows = boardState.match(/.{1,7}/g) || [];
    return rows.map(row => row.split(''));
  }

private updateGameMessage(state: GameUpdate): void {
    if (state.error && state.nextPlayer === this.myDisc) {
      this.gameMessage = `Error: ${state.error}`;
      this.isLoser = false;

      setTimeout(() => {
        if (this.gameStatus === 'IN_PROGRESS' && this.isMyTurn) {
          this.gameMessage = "It's your turn!";
        }
      }, 3000);
      return;
    }

    if (state.status === 'FINISHED') {
      this.isLoser = false;

      if (state.origin === 'CANCELLED_NO_SHOW') {
        this.gameMessage = "Connection issues for the other player, the match is canceled.";
        return;
      }

      if (state.winner === this.myDisc) {
        this.gameMessage = "You won!";
      } else if (state.winner === null) {
        this.gameMessage = "It's a draw!";
      } else {
        this.gameMessage = "You lost...";
        this.isLoser = true;
      }
      return;
    }

    if (this.isMyTurn) {
      this.gameMessage = "It's your turn!";
    } else {
      this.gameMessage = "Opponent's turn...";
    }
  }

  playMove(colIndex: number): void {
    if (!this.isMyTurn || this.gameStatus === 'FINISHED') {
      return;
    }
    
    console.log(`[ACTION] Clic sur colonne ${colIndex}`);
    this.gameService.makeMove(colIndex);
  }

  leaveGame(): void {
    this.gameService.leaveGame(); 
  }

  private updateLastMove(newBoardState: string): void {
    const newBoard = newBoardState.split('');
    const oldBoard = this.board.flat();
    
    if (oldBoard.length === 0) return; 

    for (let i = 0; i < newBoard.length; i++) {
      if (newBoard[i] !== '_' && oldBoard[i] === '_') {
        this.lastMove = {
          row: Math.floor(i / 7),
          col: i % 7
        };
        setTimeout(() => {
          this.lastMove = null;
        }, 500);
        return;
      }
    }
  }

  isLastMove(r: number, c: number): boolean {
    return !!this.lastMove && this.lastMove.row === r && this.lastMove.col === c;
  }

  returnToProfile(): void {
    this.gameService.leaveGame();
    this.router.navigate(['/home']);
  }
  
  ngOnDestroy(): void {
    this.stateSub?.unsubscribe();
    this.errorSub?.unsubscribe();
  
    if (this.gameStatus === 'IN_PROGRESS') {
      this.gameService.leaveGame();
    }
  }
}