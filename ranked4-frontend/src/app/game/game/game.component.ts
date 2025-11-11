import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { filter, Observable, Subscription, timer } from 'rxjs';
import { GameUpdate, PlayerDisc } from './game.model';
import { GameService } from './game.service';
import { UserProfile } from '../../profile/profile.model';
import { ProfileService } from '../../profile/profile.service';
import { Gif, GifReactionEvent } from '../gif/gif.model';
import { GifService } from '../gif/gif.service';

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
  private gifService = inject(GifService);

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

  gifs: Gif[] = [];
  lastReactionsByPlayer: { [playerId: string]: GifReactionEvent | null } = {};
  private subs: Subscription[] = [];

  ngOnInit(): void {
    const gifsSub = this.gifService.getGifs().subscribe(gifs => {
      this.gifs = gifs;
    });
    this.subs.push(gifsSub);

    const gifReactSub = this.gameService.gifReactions$
      .pipe(filter((event): event is GifReactionEvent => !!event))
      .subscribe(event => {
        this.lastReactionsByPlayer[event.playerId] = event;

        const currentTimestamp = event.timestamp;
        const clearSub = timer(3000).subscribe(() => {
          const current = this.lastReactionsByPlayer[event.playerId];
          if (current && current.timestamp === currentTimestamp) {
            this.lastReactionsByPlayer[event.playerId] = null;
          }
        });
        this.subs.push(clearSub);
      });
    this.subs.push(gifReactSub);

    this.gameState$ = this.gameService.gameState$;
    
    this.myDisc = this.gameService.getMyPlayerDisc();
    this.myProfile$ = this.profileService.getProfile();
    
    this.playSound('match-found.mp3');

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
      const newIsMyTurn = state.nextPlayer === this.myDisc && state.status === 'IN_PROGRESS';
      
      if (newIsMyTurn && !this.isMyTurn) {
        if (this.board.length > 0) {
          this.playSound('your-turn.mp3');
        }
      }
      this.isMyTurn = newIsMyTurn;
      
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

    if (oldBoard.length === 0) {
      this.lastMove = null;
      return;
    }

    for (let i = 0; i < newBoard.length; i++) {
      if (newBoard[i] !== '_' && oldBoard[i] !== newBoard[i]) {
        this.lastMove = {
          row: Math.floor(i / 7),
          col: i % 7
        };
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

  onGifClick(gif: Gif): void {
    this.gameService.sendGifReaction(gif.code);
  }

  playSound(soundFile: string): void {
    try {
      const audio = new Audio(`assets/sounds/${soundFile}`);
      audio.volume = 0.05;
      
      audio.play().catch(e => {
        console.warn(`[Audio] N'a pas pu jouer le son "${soundFile}". Une interaction utilisateur est peut-être requise.`, e);
      });
    } catch (e) {
      console.error(`[Audio] Erreur lors de l'initialisation du son "${soundFile}".`, e);
    }
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
    this.stateSub?.unsubscribe();
    this.errorSub?.unsubscribe();
  
    if (this.gameStatus === 'IN_PROGRESS') {
      this.gameService.leaveGame();
    }
  }
}