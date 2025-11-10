import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { LoginService } from './security/login/login.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule, RouterLink],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  title = 'ranked4-frontend';

  authService = inject(LoginService);
  private router = inject(Router);

  public isMenuOpen = false;

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
    this.isMenuOpen = false;
  }

  playRankedGame(): void {
    this.router.navigate(['/matchmaking']);
    this.isMenuOpen = false;
  }

  playPrivateGame(): void {
    this.router.navigate(['/private-game']);
    this.isMenuOpen = false;
  }
}
