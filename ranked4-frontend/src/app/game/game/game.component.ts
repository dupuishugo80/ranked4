import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { filter, Observable, Subscription, timer } from 'rxjs';
import { GameUpdate, PlayerDisc, PlayerInfo } from './game.model';
import { GameService } from './game.service';
import { DiscCustomization, UserProfile } from '../../profile/profile.model';
import { Gif, GifReactionEvent } from '../gif/gif.model';
import { GifService } from '../gif/gif.service';

@Component({
  selector: 'app-game',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game.component.html',
  styleUrl: './game.component.scss'
})
export class GameComponent implements OnInit, OnDestroy {
  
  public gameService = inject(GameService);
  private router = inject(Router);
  private gifService = inject(GifService);

  public playerOneDiscStyle: { [key: string]: string } = {};
  public playerTwoDiscStyle: { [key: string]: string } = {};

  public myPlayerInfo: PlayerInfo | null = null;
  public opponentPlayerInfo: PlayerInfo | null = null;

  private readonly DEFAULT_P1_COLOR = '#dc3545';
  private readonly DEFAULT_P2_COLOR = '#ffc107';

  private playerOneDisc: DiscCustomization | null = null;
  private playerTwoDisc: DiscCustomization | null = null;

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
    
    this.playSound('match-found.mp3');

    this.stateSub = this.gameState$.pipe(filter(state => state !== null)).subscribe(state => {
      if (!state) return;
      if (this.myDisc === 'PLAYER_ONE') {
        this.myPlayerInfo = state.playerOne;
        this.opponentPlayerInfo = state.playerTwo;
      } else {
        this.myPlayerInfo = state.playerTwo;
        this.opponentPlayerInfo = state.playerOne;
      }

      this.playerOneDisc = state.playerOne.disc;
      this.playerTwoDisc = state.playerTwo.disc;

      this.playerOneDiscStyle = this.createDiscStyle(state.playerOne.disc, 'P1');
      this.playerTwoDiscStyle = this.createDiscStyle(state.playerTwo.disc, 'P2');

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
    const frontendBoardState = boardState.replace(/0/g, '_');
    const rows = frontendBoardState.match(/.{1,7}/g) || [];
    return rows.map(row => row.split(''));
  }

  private updateGameMessage(state: GameUpdate): void {
    console.log('[GAME STATE]', state);
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
    const newBoardParsed = newBoardState.replace(/0/g, '_').split('');
    const oldBoard = this.board.flat();

    if (oldBoard.length === 0) {
      this.lastMove = null;
      return;
    }

  for (let i = 0; i < newBoardParsed.length; i++) {
      if (newBoardParsed[i] !== '_' && oldBoard[i] !== newBoardParsed[i]) {
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

  private areDiscsIdentical(): boolean {
    if (!this.playerOneDisc && !this.playerTwoDisc) {
      return true;
    }

    if (!this.playerOneDisc || !this.playerTwoDisc) {
      return false;
    }

    if (this.playerOneDisc.type !== this.playerTwoDisc.type) {
      return false;
    }

    return this.playerOneDisc.value === this.playerTwoDisc.value;
  }

  private createDiscStyle(
    disc: DiscCustomization | null,
    defaultPlayer: 'P1' | 'P2'
  ): { [key: string]: string } {
    const style: { [key: string]: string } = {};
    const areIdentical = this.areDiscsIdentical();

    if (!disc) {
      const defaultColor = defaultPlayer === 'P1' ? this.DEFAULT_P1_COLOR : this.DEFAULT_P2_COLOR;
      style['background-color'] = defaultColor;
    } else if (disc.type === 'color') {
      style['background-color'] = disc.value;
    } else if (disc.type === 'image') {
      style['background-image'] = `url(${disc.value})`;
      style['background-size'] = 'cover';
      style['background-position'] = 'center';
      style['background-repeat'] = 'no-repeat';
    } else {
      const defaultColor = defaultPlayer === 'P1' ? this.DEFAULT_P1_COLOR : this.DEFAULT_P2_COLOR;
      style['background-color'] = defaultColor;
    }

    if (areIdentical) {
      const borderColor = defaultPlayer === 'P1' ? '#ffc107' : '#dc3545';
      style['border'] = `4px solid ${borderColor}`;
      style['box-shadow'] = `0 0 0 2px rgba(0, 0, 0, 0.1), 0 0 12px ${borderColor}`;
    }

    return style;
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