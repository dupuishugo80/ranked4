import { CommonModule } from "@angular/common";
import { Component, OnInit, inject, signal, WritableSignal } from "@angular/core";
import { NavigationEnd, Router } from "@angular/router";
import { BehaviorSubject, filter, Observable, Subject, Subscription, takeUntil } from "rxjs";
import { LeaderboardComponent } from "../leaderboard/leaderboard.component";
import { ProfileService } from "../profile/profile.service";
import { UserProfile } from "../profile/profile.model";
import { GameHistoryComponent } from "../game-history/game-history.component";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, LeaderboardComponent, GameHistoryComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  private profileService = inject(ProfileService);
  private router = inject(Router);

  private navigationSubscription: Subscription | null = null;

  public reloadTrigger$ = new BehaviorSubject<void>(undefined);
  public userProfile$ = new BehaviorSubject<UserProfile | null>(null);
  public showDailyFreeModal: WritableSignal<boolean> = signal(false);

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.fetchData();
    this.checkDailyFree();

    this.navigationSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      filter(event => (event as NavigationEnd).url === '/' || (event as NavigationEnd).urlAfterRedirects === '/')
    ).subscribe(() => {
      this.fetchData();
      this.checkDailyFree();
    });
  }

  fetchData(): void {
    this.profileService.getProfile().pipe(
      takeUntil(this.destroy$)
    ).subscribe(profile => {
      this.userProfile$.next(profile);
    });

    this.reloadTrigger$.next();
  }

  checkDailyFree(): void {
    this.profileService.isDailyFreeAvailable().pipe(
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response) => {
        if (response.available) {
          this.showDailyFreeModal.set(true);
        }
      },
      error: (err) => console.error('Error checking daily free:', err)
    });
  }

  closeDailyFreeModal(): void {
    this.showDailyFreeModal.set(false);
  }

  goToShop(): void {
    this.closeDailyFreeModal();
    this.router.navigate(['/shop']);
  }

  ngOnDestroy(): void {
    if (this.navigationSubscription) {
      this.navigationSubscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();
  }
}