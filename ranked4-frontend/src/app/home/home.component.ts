import { CommonModule } from "@angular/common";
import { Component, OnInit, inject } from "@angular/core";
import { NavigationEnd, Router } from "@angular/router";
import { BehaviorSubject, filter, Observable, Subject, Subscription, takeUntil } from "rxjs";
import { LeaderboardComponent } from "../leaderboard/leaderboard.component";
import { ProfileService } from "../profile/profile.service";
import { UserProfile } from "../profile/profile.model";

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, LeaderboardComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  private profileService = inject(ProfileService);
  private router = inject(Router);

  private navigationSubscription: Subscription | null = null;

  public reloadTrigger$ = new BehaviorSubject<void>(undefined);
  public userProfile$ = new BehaviorSubject<UserProfile | null>(null);

  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.fetchData();

    this.navigationSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd),
      filter(event => (event as NavigationEnd).url === '/' || (event as NavigationEnd).urlAfterRedirects === '/')
    ).subscribe(() => {
      this.fetchData();
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

  ngOnDestroy(): void {
    if (this.navigationSubscription) {
      this.navigationSubscription.unsubscribe();
    }

    this.destroy$.next();
    this.destroy$.complete();
  }
}