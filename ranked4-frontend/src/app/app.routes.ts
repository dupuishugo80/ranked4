import { Routes } from '@angular/router';
import { LoginComponent } from './security/login/login.component';
import { RegisterComponent } from './security/register/register.component';
import { authGuard } from './guards/auth.guard';
import { MatchmakingComponent } from './game/matchmaking/matchmaking.component';
import { GameComponent } from './game/game/game.component';
import { HomeComponent } from './home/home.component';

export const routes: Routes = [
    { 
    path: '', 
    redirectTo: '/home', 
    pathMatch: 'full' 
  },
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'register',
    component: RegisterComponent
  },
  {
    path: 'matchmaking',
    component: MatchmakingComponent,
    canActivate: [authGuard]
  },
  {
    path: 'game/:id',
    component: GameComponent,
    canActivate: [authGuard]
  },
  {
    path: 'home',
    component: HomeComponent,
    canActivate: [authGuard]
  }
];
