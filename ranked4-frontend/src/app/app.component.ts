import { CommonModule } from '@angular/common';
import { Component, ElementRef, HostListener, inject, OnInit, signal, ViewChild, WritableSignal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { LoginService } from './security/login/login.service';
import { ProfileService } from './profile/profile.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule, RouterLink],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent implements OnInit {
  title = 'ranked4-frontend';

  authService = inject(LoginService);
  private router = inject(Router);
  private profileService = inject(ProfileService);

  public isMenuOpen = false;
  public isPlayMenuOpen = false;
  public userGold: WritableSignal<number> = signal(0);

  public get isAdmin(): boolean {
    return this.authService.isAdmin();
  }

  @ViewChild('dropdownMenu') dropdownMenu!: ElementRef;

  @HostListener('document:click', ['$event'])
  onClick(event: MouseEvent) {
    const clickedInside = this.dropdownMenu.nativeElement.contains(event.target);
    if (!clickedInside) {
      this.togglePlayMenu(false);
    }
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.loadUserProfile();
    }
  }

  private loadUserProfile(): void {
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.userGold.set(profile.gold);
      },
      error: (err) => console.error('Error loading user profile:', err)
    });
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  togglePlayMenu(newValue?: boolean): void {
    if (newValue !== undefined) {
      this.isPlayMenuOpen = newValue;
      return;
    }

    this.isPlayMenuOpen = !this.isPlayMenuOpen;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.isMenuOpen = false;
  }

  playRankedGame(): void {
    this.router.navigate(['/matchmaking']);
    this.isMenuOpen = false;
    this.isPlayMenuOpen = false;
  }

  playPrivateGame(): void {
    this.router.navigate(['/private-game']);
    this.isMenuOpen = false;
    this.isPlayMenuOpen = false;
  }

  playAiGame(): void {
    this.router.navigate(['/ai-game']);
    this.isMenuOpen = false;
    this.isPlayMenuOpen = false;
  }

  goToAdmin(): void {
    this.router.navigate(['/admin']);
    this.isMenuOpen = false;
  }

  goToShop(): void {
    this.router.navigate(['/shop']);
    this.isMenuOpen = false;
    this.isPlayMenuOpen = false;
  }

  goToProfile(): void {
    this.router.navigate(['/profile']);
    this.isMenuOpen = false;
    this.isPlayMenuOpen = false;
  }
}
