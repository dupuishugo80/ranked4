import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { filter, interval, Observable, Subscription, timer } from 'rxjs';
import { GameUpdate, PlayerDisc, PlayerInfo } from './game.model';
import { GameService } from './game.service';
import { DiscCustomization, UserProfile } from '../../profile/profile.model';
import { Gif, GifReactionEvent } from '../gif/gif.model';
import { GifService } from '../gif/gif.service';
import { ProfileService } from '../../profile/profile.service';

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
  private route = inject(ActivatedRoute);
  private gifService = inject(GifService);
  private profileService = inject(ProfileService);

  public playerOneDiscStyle: { [key: string]: string } = {};
  public playerTwoDiscStyle: { [key: string]: string } = {};

  public myPlayerInfo: PlayerInfo | null = null;
  public opponentPlayerInfo: PlayerInfo | null = null;

  private initialElo: number = 0;

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
  public gameMessage: string = 'Loading the game...';
  public goldEarned: number | null = null;
  public eloChange: number | null = null;
  public turnTimeRemaining: number | null = null;

  private stateSub: Subscription | null = null;
  private errorSub: Subscription | null = null;
  private opponentIdSet = false;

  public lastMove: { row: number; col: number } | null = null;

  public isLoser: boolean = false;

  gifs: Gif[] = [];
  lastReactionsByPlayer: { [playerId: string]: GifReactionEvent | null } = {};
  private subs: Subscription[] = [];

  private timerSub: Subscription | null = null;
  private lastReceivedTime: number | null = null;

  ngOnInit(): void {
    const gameId = this.route.snapshot.paramMap.get('id');

    if (gameId && this.gameService.gameStatus$.value !== 'IN_GAME') {
      this.gameService.joinGameById(gameId);
    }

    const gifsSub = this.gifService.getGifs().subscribe((gifs) => {
      this.gifs = gifs;
    });
    this.subs.push(gifsSub);

    const gifReactSub = this.gameService.gifReactions$.pipe(filter((event): event is GifReactionEvent => !!event)).subscribe((event) => {
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

    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.initialElo = profile.elo;
      },
      error: (err) => console.error('Error loading initial profile:', err)
    });

    this.playSound('match-found.mp3');

    this.stateSub = this.gameState$.pipe(filter((state) => state !== null)).subscribe((state) => {
      if (!state) return;

      // Auto-detect myDisc if not set yet
      if (!this.myDisc) {
        const myUserId = this.gameService.getMyUserId();
        if (myUserId) {
          if (String(state.playerOne.userId) === String(myUserId)) {
            this.myDisc = 'PLAYER_ONE';
          } else if (String(state.playerTwo.userId) === String(myUserId)) {
            this.myDisc = 'PLAYER_TWO';
          }
        }
      }

      if (this.myDisc === 'PLAYER_ONE') {
        this.myPlayerInfo = state.playerOne;
        this.opponentPlayerInfo = this.formatAiOpponent(state.playerTwo);
      } else {
        this.myPlayerInfo = state.playerTwo;
        this.opponentPlayerInfo = this.formatAiOpponent(state.playerOne);
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

      if (state.turnTimeRemainingSeconds !== undefined && state.turnTimeRemainingSeconds !== null) {
        this.lastReceivedTime = state.turnTimeRemainingSeconds;
        this.turnTimeRemaining = state.turnTimeRemainingSeconds;
        this.startLocalTimer();
      } else {
        this.turnTimeRemaining = null;
        this.stopLocalTimer();
      }
    });
  }

  private startLocalTimer(): void {
    this.stopLocalTimer();

    this.timerSub = interval(1000).subscribe(() => {
      if (this.turnTimeRemaining !== null && this.turnTimeRemaining > 0) {
        this.turnTimeRemaining--;
      }
    });
  }

  private stopLocalTimer(): void {
    if (this.timerSub) {
      this.timerSub.unsubscribe();
      this.timerSub = null;
    }
  }

  private formatAiOpponent(playerInfo: PlayerInfo): PlayerInfo {
    const AI_UUID = '00000000-0000-0000-0000-000000000001';

    if (playerInfo.userId === AI_UUID) {
      return {
        ...playerInfo,
        displayName: 'IA',
        avatarUrl: 'https://img.freepik.com/vecteurs-libre/robot-vectoriel-graident-ai_78370-4114.jpg?semt=ais_se_enriched&w=740&q=80',
        elo: 0
      };
    }

    return playerInfo;
  }

  isOpponentP1(): boolean {
    return this.myDisc === 'PLAYER_TWO';
  }

  private parseBoardState(boardState: string): string[][] {
    const frontendBoardState = boardState.replace(/0/g, '_');
    const rows = frontendBoardState.match(/.{1,7}/g) || [];
    return rows.map((row) => row.split(''));
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
        this.gameMessage = 'Connection issues for the other player, the match is canceled.';
        this.goldEarned = null;
        this.eloChange = null;
        return;
      }

      const isRanked = state.origin === 'RANKED';
      const isPve = state.origin === 'PVE';

      let goldWin: number;
      let goldDraw: number;

      if (isPve) {
        // PVE rewards based on AI difficulty: 1=Easy, 2=Medium, 3=Hard
        const difficulty = state.aiDifficulty || 2; // Default to medium
        switch (difficulty) {
          case 1: // Easy
            goldWin = 50;
            goldDraw = 25;
            break;
          case 2: // Medium
            goldWin = 100;
            goldDraw = 50;
            break;
          case 3: // Hard
            goldWin = 200;
            goldDraw = 100;
            break;
          default:
            goldWin = 100;
            goldDraw = 50;
        }
      } else {
        goldWin = isRanked ? 100 : 50;
        goldDraw = isRanked ? 50 : 25;
      }

      // Déterminer le message et le gold gagné
      if (state.winner === this.myDisc) {
        this.gameMessage = 'You won!';
        this.goldEarned = goldWin;
      } else if (state.winner === null) {
        this.gameMessage = "It's a draw!";
        this.goldEarned = goldDraw;
      } else {
        this.gameMessage = 'You lost...';
        this.goldEarned = 0;
        this.isLoser = true;
      }

      if (isRanked) {
        setTimeout(() => {
          this.profileService.getProfile().subscribe({
            next: (profile) => {
              this.eloChange = profile.elo - this.initialElo;
            },
            error: (err) => {
              console.error('Error loading profile for ELO calculation:', err);
              this.eloChange = null;
            }
          });
        }, 1000);
      } else {
        this.eloChange = null;
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

      audio.play().catch((e) => {
        console.warn(`[Audio] N'a pas pu jouer le son "${soundFile}". Une interaction utilisateur est peut-être requise.`, e);
      });
    } catch (e) {
      console.error(`[Audio] Erreur lors de l'initialisation du son "${soundFile}".`, e);
    }
  }

  private areDiscsIdentical(): boolean {
    if (!this.playerOneDisc && !this.playerTwoDisc) {
      return false;
    }

    if (!this.playerOneDisc || !this.playerTwoDisc) {
      return false;
    }

    if (this.playerOneDisc.type !== this.playerTwoDisc.type) {
      return false;
    }

    return this.playerOneDisc.value === this.playerTwoDisc.value;
  }

  private createDiscStyle(disc: DiscCustomization | null, defaultPlayer: 'P1' | 'P2'): { [key: string]: string } {
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
    this.subs.forEach((s) => s.unsubscribe());
    this.stateSub?.unsubscribe();
    this.errorSub?.unsubscribe();
    this.stopLocalTimer();

    // Only cleanup if the game is still in progress
    // Finished games will be cleaned up when starting a new game
    if (this.gameStatus === 'IN_PROGRESS') {
      this.gameService.leaveGame();
    }
  }
}
