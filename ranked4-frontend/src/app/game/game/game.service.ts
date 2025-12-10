import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Subject, Subscription, filter, interval, take } from 'rxjs';
import { WebSocketService } from '../websocket/websocket.service';
import { GameUpdate, PlayerDisc } from './game.model';
import { API_ENDPOINTS } from '../../core/config/api.config';
import { LoginService } from '../../security/login/login.service';
import { GifReactionEvent } from '../gif/gif.model';

type GameStatus = 'IDLE' | 'QUEUEING' | 'IN_GAME' | 'FINISHED';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private http = inject(HttpClient);
  private wsService = inject(WebSocketService);
  private authService = inject(LoginService);
  private router = inject(Router);

  private readonly API_URL_MATCHMAKING_JOIN = API_ENDPOINTS.MATCHMAKING_JOIN;
  private readonly API_URL_MATCHMAKING_LEAVE = API_ENDPOINTS.MATCHMAKING_LEAVE;
  private readonly API_URL_PRIVATE_CREATE = API_ENDPOINTS.PRIVATE_MATCHES;
  private readonly API_URL_PRIVATE_JOIN = API_ENDPOINTS.PRIVATE_MATCHES_JOIN;
  private readonly API_URL_PRIVATE_START = API_ENDPOINTS.PRIVATE_MATCHES_START;
  private readonly API_URL_PRIVATE_LOBBY = API_ENDPOINTS.PRIVATE_MATCHES_LOBBY;

  private readonly LOBBY_TOPIC = '/topic/lobby';
  private readonly LOBBY_REGISTER_DEST = '/app/lobby.register';

  private lobbySub: Subscription | null = null;
  private gameSub: Subscription | null = null;
  private matchmakingTimeout: any = null;
  private queueTimerSub: Subscription | null = null;
  private queueTimerInterval: any = null;
  private initialJoinTimeout: any = null;

  public gameState$ = new BehaviorSubject<GameUpdate | null>(null);
  public gameStatus$ = new BehaviorSubject<GameStatus>('IDLE');
  public gameError$ = new Subject<string>();
  public queueTime$ = new BehaviorSubject<number>(0);
  private currentPrivateCodeSubject = new BehaviorSubject<string | null>(null);
  public hasGuestJoined$ = new BehaviorSubject<boolean>(false);
  public currentPrivateCode$ = this.currentPrivateCodeSubject.asObservable();
  private gifReactionsSubject = new Subject<GifReactionEvent>();
  public gifReactions$ = this.gifReactionsSubject.asObservable();

  private myUserId: string | null = null;
  private myGameId: string | null = null;
  private myPlayerDisc: PlayerDisc | null = null;

  constructor() {
    this.myUserId = this.authService.getUserId();
  }

  joinGameById(gameId: string): void {
    if (!this.myUserId) {
      console.error('[DIRECT JOIN] User ID is null');
      return;
    }

    if (this.gameStatus$.value === 'FINISHED') {
      this.cleanUp();
    }

    this.myGameId = gameId;
    this.gameStatus$.next('IN_GAME');

    this.wsService.connect();

    this.wsService.connectionState
      .pipe(
        filter((state) => state === 'CONNECTED'),
        take(1)
      )
      .subscribe(() => {
        this.subscribeToGameTopic(gameId);
        this.sendJoinMessage(gameId, this.myUserId!);
      });
  }

  joinQueue(): void {
    if (this.gameStatus$.value === 'FINISHED') {
      this.resetState();
    }

    if (this.gameStatus$.value === 'QUEUEING' || this.gameStatus$.value === 'IN_GAME') {
      return;
    }

    this.gameStatus$.next('QUEUEING');
    this.startQueueTimer();
    this.wsService.connect();

    this.wsService.connectionState
      .pipe(
        filter((state) => state === 'CONNECTED'),
        take(1)
      )
      .subscribe(() => {
        this.wsService.publishMessage(this.LOBBY_REGISTER_DEST, {
          playerId: this.myUserId
        });

        this.subscribeToLobby();

        this.initialJoinTimeout = setTimeout(() => {
          if (this.gameStatus$.value !== 'QUEUEING') {
            return;
          }

          this.http.post(this.API_URL_MATCHMAKING_JOIN, {}).subscribe({
            next: () => {
              this.matchmakingTimeout = setTimeout(() => {
                if (this.gameStatus$.value === 'QUEUEING') {
                  this.gameError$.next('Aucun adversaire trouvé. Le matchmaking a expiré.');
                  this.leaveGame();
                }
              }, 300000);
            },
            error: (err) => {
              console.error('[HTTP] Erreur pour rejoindre la file:', err);
              this.gameStatus$.next('IDLE');
              this.cleanUp();
            }
          });
        }, 200);
      });
  }

  createPrivateMatch(): void {
    if (this.gameStatus$.value === 'FINISHED') {
      this.resetState();
    }

    if (this.gameStatus$.value !== 'IDLE') return;

    this.gameStatus$.next('QUEUEING');
    this.wsService.connect();

    this.wsService.connectionState
      .pipe(
        filter((state) => state === 'CONNECTED'),
        take(1)
      )
      .subscribe(() => {
        this.wsService.publishMessage(this.LOBBY_REGISTER_DEST, {
          playerId: this.myUserId
        });

        this.subscribeToLobby();

        this.http.post<{ code: string; expiresInSeconds: number }>(this.API_URL_PRIVATE_CREATE, {}).subscribe({
          next: (res) => {
            this.currentPrivateCodeSubject.next(res.code);
          },
          error: (err) => {
            console.error('[PRIVATE] Failed to create private match', err);
            this.gameStatus$.next('IDLE');
            this.cleanUp();
          }
        });
      });
  }

  joinPrivateMatch(code: string): void {
    if (this.gameStatus$.value === 'FINISHED') {
      this.resetState();
    }

    if (this.gameStatus$.value !== 'IDLE') return;

    const normalized = code.trim().toUpperCase();
    if (!normalized) return;

    this.gameStatus$.next('QUEUEING');
    this.wsService.connect();

    this.wsService.connectionState
      .pipe(
        filter((state) => state === 'CONNECTED'),
        take(1)
      )
      .subscribe(() => {
        this.wsService.publishMessage(this.LOBBY_REGISTER_DEST, {
          playerId: this.myUserId
        });

        this.subscribeToLobby();

        this.http.post(this.API_URL_PRIVATE_JOIN, { code: normalized }).subscribe({
          next: (res: any) => {},
          error: (err) => {
            console.error('[PRIVATE] Failed to join private lobby', err);
            this.gameStatus$.next('IDLE');
            this.cleanUp();
          }
        });
      });
  }

  startPrivateMatch(): void {
    const code = this.currentPrivateCodeSubject.value;
    if (!code) {
      console.warn('[PRIVATE] No code to start');
      return;
    }

    this.http.post<{ matchId: string }>(this.API_URL_PRIVATE_START, { code }).subscribe({
      next: (res) => {},
      error: (err) => {
        console.error('[PRIVATE] Failed to start private match', err);
      }
    });
  }

  checkPrivateLobby(): void {
    const code = this.currentPrivateCodeSubject.value;
    if (!code) return;

    this.http.get<{ hostUserId: string; guestUserId: string | null }>(`${this.API_URL_PRIVATE_LOBBY}/${code}`).subscribe({
      next: (res) => {
        this.hasGuestJoined$.next(!!res.guestUserId);
      },
      error: () => {
        this.hasGuestJoined$.next(false);
      }
    });
  }

  private subscribeToLobby(): void {
    if (this.lobbySub) {
      return;
    }

    this.lobbySub = this.wsService.subscribeToTopic(this.LOBBY_TOPIC).subscribe((message) => {
      const gameUpdate: GameUpdate = JSON.parse(message.body);

      if (!this.myUserId) {
        console.error('myUserId est null, impossible de confirmer le match.');
        return;
      }

      const isMe = String(gameUpdate.playerOne.userId) === String(this.myUserId) || String(gameUpdate.playerTwo.userId) === String(this.myUserId);

      if (!isMe) {
        return;
      }

      if (this.matchmakingTimeout) {
        clearTimeout(this.matchmakingTimeout);
        this.matchmakingTimeout = null;
      }

      this.myGameId = gameUpdate.gameId;
      this.myPlayerDisc = String(gameUpdate.playerOne.userId) === String(this.myUserId) ? 'PLAYER_ONE' : 'PLAYER_TWO';

      this.gameStatus$.next('IN_GAME');
      this.gameState$.next(gameUpdate);

      this.sendJoinMessage(this.myGameId, this.myUserId!);
      this.subscribeToGameTopic(this.myGameId);

      this.router.navigate(['/game', this.myGameId]);

      this.lobbySub?.unsubscribe();
      this.lobbySub = null;
    });
  }

  private startQueueTimer(): void {
    this.stopQueueTimer();
    this.queueTime$.next(0);
    this.queueTimerInterval = setInterval(() => {
      this.queueTime$.next(this.queueTime$.value + 1);
    }, 1000);
  }

  private stopQueueTimer(): void {
    if (this.queueTimerInterval) {
      clearInterval(this.queueTimerInterval);
      this.queueTimerInterval = null;
    }
  }

  private sendJoinMessage(gameId: string, playerId: string): void {
    if (!gameId || !playerId) {
      console.error("Impossible de rejoindre, ID de partie ou d'utilisateur manquant.");
      return;
    }
    this.wsService.publishMessage(`/app/game.join/${gameId}`, {
      playerId: playerId
    });
  }

  private subscribeToGameTopic(gameId: string): void {
    if (this.gameSub) return;

    const gameTopic = `/topic/game/${gameId}`;
    this.gameSub = this.wsService.subscribeToTopic(gameTopic).subscribe((message) => {
      const gameUpdate: GameUpdate = JSON.parse(message.body);

      if (!this.myPlayerDisc && this.myUserId) {
        if (String(gameUpdate.playerOne.userId) === String(this.myUserId)) {
          this.myPlayerDisc = 'PLAYER_ONE';
        } else if (String(gameUpdate.playerTwo.userId) === String(this.myUserId)) {
          this.myPlayerDisc = 'PLAYER_TWO';
        }
      }

      if (gameUpdate.error) {
        console.error(`ERREUR DE JEU: ${gameUpdate.error}`);
      }

      if (gameUpdate.status === 'FINISHED') {
        this.gameStatus$.next('FINISHED');
        this.cleanUp();
      }

      this.gameState$.next(gameUpdate);
    });
    this.wsService.subscribeToTopic(`/topic/game/${gameId}/gif`).subscribe((message) => {
      try {
        const event = JSON.parse(message.body) as GifReactionEvent;
        this.gifReactionsSubject.next(event);
      } catch (e) {
        console.error('Invalid GIF reaction event', e);
      }
    });
  }

  makeMove(column: number): void {
    if (this.gameStatus$.value !== 'IN_GAME' || !this.myGameId || !this.myUserId) {
      console.error('Impossible de jouer, pas dans une partie.');
      return;
    }

    this.wsService.publishMessage(`/app/game.move/${this.myGameId}`, {
      gameId: this.myGameId,
      playerId: this.myUserId,
      column: column
    });
  }

  sendGifReaction(gifCode: string): void {
    const gameId = this.myGameId;
    const playerId = this.myUserId;

    if (!gameId || !playerId || this.gameStatus$.value !== 'IN_GAME') {
      return;
    }

    this.wsService.connectionState
      .pipe(
        filter((state) => state !== 'CONNECTED'),
        take(1)
      )
      .subscribe(() => {
        console.warn('[GIF] WebSocket non connecté, réaction ignorée');
        return;
      });

    const payload = { gameId, playerId, gifCode };
    this.wsService.publishMessage(`/app/game.gif/${gameId}`, payload);
  }

  startNewMatchmakingSession(): void {
    this.cleanUp();
    this.gameStatus$.next('IDLE');
    this.gameState$.next(null);
  }

  leaveGame(): void {
    if (this.gameStatus$.value === 'QUEUEING') {
      this.http.post(this.API_URL_MATCHMAKING_LEAVE, {}).subscribe({
        next: () => {},
        error: (err) => console.error('[HTTP] Erreur pour quitter la file:', err)
      });
    }

    this.cleanUp();
    this.gameStatus$.next('IDLE');
    this.gameState$.next(null);
  }

  resetState(): void {
    this.cleanUp();
    this.gameStatus$.next('IDLE');
    this.gameState$.next(null);
    this.currentPrivateCodeSubject.next(null);
  }

  private cleanUp(): void {
    if (this.initialJoinTimeout) {
      clearTimeout(this.initialJoinTimeout);
      this.initialJoinTimeout = null;
    }

    if (this.matchmakingTimeout) {
      clearTimeout(this.matchmakingTimeout);
      this.matchmakingTimeout = null;
    }

    this.queueTimerSub?.unsubscribe();
    this.queueTimerSub = null;
    this.queueTime$.next(0);

    this.lobbySub?.unsubscribe();
    this.gameSub?.unsubscribe();
    this.lobbySub = null;
    this.gameSub = null;

    this.wsService.disconnect();

    this.myGameId = null;
    this.myPlayerDisc = null;
  }

  public getMyPlayerDisc(): PlayerDisc | null {
    return this.myPlayerDisc;
  }

  public getMyUserId(): string | null {
    return this.myUserId;
  }

  public getCurrentGameId(): string | null {
    return this.myGameId;
  }
}
