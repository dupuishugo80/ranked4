import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';
import { GameService } from '../game/game.service';
import { LoginService } from '../../security/login/login.service';

@Component({
selector: 'app-private-game',
standalone: true,
imports: [CommonModule, FormsModule],
templateUrl: './private-game.component.html',
styleUrl: './private-game.component.scss'
})
export class PrivateGameComponent implements OnInit, OnDestroy {

    public gameService = inject(GameService);
    private authService = inject(LoginService);

    status: string = '';
    privateCodeInput: string = '';
    createdCode: string | null = null;
    isHost: boolean = false;
    guestJoined: boolean = false;

    private statusSub?: Subscription;
    private codeSub?: Subscription;

    ngOnInit(): void {
        this.gameService.resetState();
        this.isHost = false;
        this.createdCode = null;

        this.statusSub = this.gameService.gameStatus$.subscribe(status => {
            switch (status) {
                case 'IDLE':
                    this.status = 'Choose to create or join a private match.';
                    break;
                case 'QUEUEING':
                    this.status = this.isHost
                        ? 'Waiting for a friend to join your code.'
                        : 'Waiting for the host to start the match.';
                    break;
                case 'IN_GAME':
                    this.status = 'Match found! Redirecting to the game...';
                    break;
                case 'FINISHED':
                    break;
            }
        });

        this.codeSub = this.gameService.currentPrivateCode$.subscribe(code => {
            this.createdCode = code;
        });

        if (this.gameService.gameStatus$.value === 'IDLE') {
            this.status = 'Choose to create or join a private match.';
        }

        this.gameService.hasGuestJoined$.subscribe(joined => {
            this.guestJoined = joined;
        });

        setInterval(() => {
            if (this.isHost && this.gameService.gameStatus$.value === 'QUEUEING' && this.createdCode) {
                this.gameService.checkPrivateLobby();
            }
        }, 1000);
    }

    createPrivate(): void {
        if (this.gameService.gameStatus$.value !== 'IDLE') return;
        this.isHost = true;
        this.gameService.resetState();
        this.gameService.createPrivateMatch();
    }

    joinPrivate(): void {
        if (!this.privateCodeInput.trim()) return;
        if (this.gameService.gameStatus$.value !== 'IDLE') return;
        this.isHost = false;
        this.gameService.resetState();
        this.gameService.joinPrivateMatch(this.privateCodeInput);
    }

    startPrivate(): void {
        if (!this.isHost) return;
        this.gameService.startPrivateMatch();
    }

    ngOnDestroy(): void {
        if (this.gameService.gameStatus$.value === 'QUEUEING') {
            this.gameService.leaveGame();
        }
        this.statusSub?.unsubscribe();
        this.codeSub?.unsubscribe();
    }
}