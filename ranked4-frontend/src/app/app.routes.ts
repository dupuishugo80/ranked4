import { Routes } from '@angular/router';
import { LoginComponent } from './security/login/login.component';
import { RegisterComponent } from './security/register/register.component';
import { authGuard } from './guards/auth.guard';
import { MatchmakingComponent } from './game/matchmaking/matchmaking.component';
import { GameComponent } from './game/game/game.component';
import { HomeComponent } from './home/home.component';
import { PrivateGameComponent } from './game/private-game/private-game.component';
import { AdminComponent } from './admin/admin.component';
import { adminGuard } from './guards/admin.guard';
import { publicGuard } from './guards/public.guard';
import { AdminUsersComponent } from './admin/admin-users/admin-users.component';
import { AdminLootboxesComponent } from './admin/admin-lootboxes/admin-lootboxes.component';
import { AdminSkinsComponent } from './admin/admin-skin/admin-skins.component';
import { AdminGifsComponent } from './admin/admin-gifs/admin-gifs.component';
import { ShopComponent } from './shop/shop.component';
import { ProfileComponent } from './profile/profile.component';
import { AiDifficultySelectionComponent } from './game/ai/ai-difficulty-selection.component';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/home',
    pathMatch: 'full'
  },
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [publicGuard]
  },
  {
    path: 'register',
    component: RegisterComponent,
    canActivate: [publicGuard]
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
  },
  {
    path: 'shop',
    component: ShopComponent,
    canActivate: [authGuard]
  },
  {
    path: 'profile/:userId',
    component: ProfileComponent,
    canActivate: [authGuard],
    runGuardsAndResolvers: 'always'
  },
  {
    path: 'profile',
    component: ProfileComponent,
    canActivate: [authGuard],
    runGuardsAndResolvers: 'always'
  },
  {
    path: 'private-game',
    component: PrivateGameComponent,
    canActivate: [authGuard]
  },
  {
    path: 'ai-game',
    component: AiDifficultySelectionComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [adminGuard]
  },
  {
    path: 'admin-users',
    component: AdminUsersComponent,
    canActivate: [adminGuard]
  },
  {
    path: 'admin-lootboxes',
    component: AdminLootboxesComponent,
    canActivate: [adminGuard]
  },
  {
    path: 'admin-skins',
    component: AdminSkinsComponent,
    canActivate: [adminGuard]
  },
  {
    path: 'admin-gifs',
    component: AdminGifsComponent,
    canActivate: [adminGuard]
  }
];
