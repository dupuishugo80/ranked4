import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Subject, Subscription, filter, interval, take } from 'rxjs';
import { WebSocketService } from '../websocket/websocket.service';
import { GameUpdate, PlayerDisc } from './game.model';
import { LoginService } from '../../security/login/login.service';

type GameStatus = 'IDLE' | 'QUEUEING' | 'IN_GAME' | 'FINISHED';

@Injectable({
  providedIn: 'root'
})
export class GameService {
  private http = inject(HttpClient);
  private wsService = inject(WebSocketService);
  private authService = inject(LoginService);
  private router = inject(Router);

  private readonly API_URL_MATCHMAKING_JOIN = 'http://localhost:8080/api/matchmaking/join';
  private readonly API_URL_MATCHMAKING_LEAVE = 'http://localhost:8080/api/matchmaking/leave';
  private readonly LOBBY_TOPIC = '/topic/lobby';

  private lobbySub: Subscription | null = null;
  private gameSub: Subscription | null = null;
  private matchmakingTimeout: any = null;
  private queueTimerSub: Subscription | null = null;
  private queueTimerInterval: any = null; 

  public gameState$ = new BehaviorSubject<GameUpdate | null>(null);
  public gameStatus$ = new BehaviorSubject<GameStatus>('IDLE');
  public gameError$ = new Subject<string>();
  public queueTime$ = new BehaviorSubject<number>(0);

  private myUserId: string | null = null;
  private myGameId: string | null = null;
  private myPlayerDisc: PlayerDisc | null = null;


  constructor() {
    this.myUserId = this.authService.getUserId();
  }

  joinQueue(): void {
    if (this.gameStatus$.value === 'QUEUEING') return;

    console.log('[HTTP] Tentative de rejoindre la file d\'attente...');
    this.gameStatus$.next('QUEUEING');
    
    this.startQueueTimer();

    this.http.post(this.API_URL_MATCHMAKING_JOIN, {}).subscribe({
      next: () => {
        console.log('[HTTP] En file d\'attente !');
        
        this.wsService.connect();

        this.wsService.connectionState.pipe(
          filter(state => state === 'CONNECTED'),
          take(1)
        ).subscribe(() => {
          console.log('[WS] Connexion établie, abonnement au lobby...');
          this.subscribeToLobby();
        });


        this.matchmakingTimeout = setTimeout(() => {
          if (this.gameStatus$.value === 'QUEUEING') {
            console.warn('[Timeout] Le matchmaking a expiré (5 minutes).');
            this.gameError$.next("Aucun adversaire trouvé. Le matchmaking a expiré.");
            this.leaveGame();
          }
        }, 300000);

      },
      error: (err) => {
        console.error('[HTTP] Erreur pour rejoindre la file:', err);
        this.gameStatus$.next('IDLE');
      }
    });
  }

  private subscribeToLobby(): void {
    if (this.lobbySub) return;

    this.lobbySub = this.wsService.subscribeToTopic(this.LOBBY_TOPIC).subscribe(message => {
      console.log('--- [LOBBY] Notification de match reçue ---');
      const gameUpdate: GameUpdate = JSON.parse(message.body);

        if (!this.myUserId) {
          console.error("myUserId est null, impossible de confirmer le match.");
          return;
        }

        if (String(gameUpdate.playerOneId) === String(this.myUserId) || 
          String(gameUpdate.playerTwoId) === String(this.myUserId)) {

          if (this.matchmakingTimeout) {
            clearTimeout(this.matchmakingTimeout);
            this.matchmakingTimeout = null;
          }
          
          this.myGameId = gameUpdate.gameId;
          this.myPlayerDisc = (gameUpdate.playerOneId === this.myUserId) ? 'PLAYER_ONE' : 'PLAYER_TWO';

          console.log(`🎉 [MATCH] C'est notre match ! ID: ${this.myGameId}`);
          console.log(`Nous sommes ${this.myPlayerDisc}.`);

          this.gameStatus$.next('IN_GAME');
          this.gameState$.next(gameUpdate);

          this.sendJoinMessage(this.myGameId, this.myUserId!);
          
          this.subscribeToGameTopic(this.myGameId);

          this.router.navigate(['/game', this.myGameId]);

          this.lobbySub?.unsubscribe();
          this.lobbySub = null;
        }
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
    if (!this.myGameId || !this.myUserId) {
      console.error("Impossible de rejoindre, ID de partie ou d'utilisateur manquant.");
      return;
    }
    console.log(`[WS] Enregistrement de la session pour la partie ${gameId}`);
    this.wsService.publishMessage(`/app/game.join/${gameId}`, {
      playerId: playerId
    });
  }

  private subscribeToGameTopic(gameId: string): void {
    if (this.gameSub) return;

    const gameTopic = `/topic/game/${gameId}`;
    this.gameSub = this.wsService.subscribeToTopic(gameTopic).subscribe(message => {
      console.log('--- [JEU] Mise à jour du plateau ---');
      const gameUpdate: GameUpdate = JSON.parse(message.body);

      if (gameUpdate.error) {
        console.error(`ERREUR DE JEU: ${gameUpdate.error}`);
      }
      
      if (gameUpdate.status === 'FINISHED') {
        console.log(`🎉 [JEU] Partie terminée ! Gagnant: ${gameUpdate.winner}`);
        this.gameStatus$.next('FINISHED');
        this.cleanUp();
      }
      
      this.gameState$.next(gameUpdate);
    });
  }

  makeMove(column: number): void {
    if (this.gameStatus$.value !== 'IN_GAME' || !this.myGameId || !this.myUserId) {
      console.error("Impossible de jouer, pas dans une partie.");
      return;
    }

    console.log(`[WS] Envoi du coup : colonne ${column}...`);
    this.wsService.publishMessage(`/app/game.move/${this.myGameId}`, {
      gameId: this.myGameId,
      playerId: this.myUserId,
      column: column
    });
  }

  leaveGame(): void {
    if (this.gameStatus$.value === 'QUEUEING') {
      console.log('[HTTP] Tentative de quitter la file d\'attente...');
      
      this.http.post(this.API_URL_MATCHMAKING_LEAVE, {}).subscribe({
        next: () => {
          console.log('[HTTP] Quitté la file d\'attente.');
        },
        error: (err) => {
          console.error('[HTTP] Erreur pour quitter la file:', err);
        }
      });
    }

    this.cleanUp();
    this.gameStatus$.next('IDLE');
    this.gameState$.next(null);
    this.router.navigate(['/home']);
  }
  
  private cleanUp(): void {
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
}