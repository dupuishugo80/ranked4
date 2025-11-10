import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { BehaviorSubject, filter, Observable, Subject, switchMap, take } from 'rxjs';
import { WEBSOCKET_URL } from '../../core/config/api.config';

type WsConnectionState = 'DISCONNECTED' | 'CONNECTING' | 'CONNECTED';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private stompClient: Client;
  private readonly WEBSOCKET_URL = WEBSOCKET_URL;

  private connectionState$ = new BehaviorSubject<WsConnectionState>('DISCONNECTED');

  public connectionState = this.connectionState$.asObservable();

  constructor() {
    this.stompClient = new Client({
      brokerURL: this.WEBSOCKET_URL,
      debug: (str) => console.log(`[STOMP] ${str}`),
      reconnectDelay: 5000,
    });

    this.stompClient.onConnect = () => {
      console.log('Connecté au WebSocket (STOMP)');
      this.connectionState$.next('CONNECTED');
    };

    this.stompClient.onStompError = (frame) => {
      console.error('[STOMP] Erreur:', frame.headers['message']);
    };
    
    this.stompClient.onWebSocketError = (error) => {
      console.error('[WS] Erreur:', error);
    };

    this.stompClient.onDisconnect = () => {
      console.log('[WS] Déconnecté');
      this.connectionState$.next('DISCONNECTED');
    };
  }

  connect(): void {
    if (this.connectionState$.value === 'DISCONNECTED') {
      this.connectionState$.next('CONNECTING');
      this.stompClient.activate();
    }
  }

  disconnect(): void {
    this.stompClient.deactivate();
  }

  subscribeToTopic(topic: string): Observable<IMessage> {
    return this.connectionState$.pipe(
      filter(state => state === 'CONNECTED'),
      take(1),
      switchMap(() => {
        return new Observable<IMessage>(observer => {
          console.log(`[WS] Abonnement à ${topic}`);
          
          const subscription: StompSubscription = this.stompClient.subscribe(topic, (message) => {
            observer.next(message);
          });
          
          return () => {
            console.log(`[WS] Désabonnement de ${topic}`);
            subscription.unsubscribe();
          };
        });
      })
    );
  }

  publishMessage(destination: string, body: any): void {
    if (this.connectionState$.value !== 'CONNECTED') {
      console.error('[WS] Impossible de publier, non connecté.');
      return;
    }
    this.stompClient.publish({
      destination: destination,
      body: JSON.stringify(body)
    });
  }
}