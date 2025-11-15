import { CommonModule } from "@angular/common";
import { Component, OnInit, inject } from "@angular/core";
import { map, Observable, of } from "rxjs";
import { UserProfile } from "../../profile/profile.model";
import { LoginService } from "../../security/login/login.service";
import { AdminUsersService } from "./admin-users.service";
import { ApiUserProfile, ApiUsersResponse } from "./admin-users.model";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {

  private adminUsersService = inject(AdminUsersService);

  public users$!: Observable<UserProfile[]>;

  ngOnInit(): void {
    this.loadUsers();
  }

  private loadUsers(): void {
    this.users$ = this.adminUsersService.getUserList().pipe(
      map((response: ApiUsersResponse) => 
        response.content.map(apiUser => this.mapToUserProfile(apiUser))
      )
    );
  }

  private mapToUserProfile(apiUser: ApiUserProfile): UserProfile {
    return {
      userId: apiUser.userId,
      id: apiUser.userId,
      displayName: apiUser.displayName,
      email: '',
      elo: apiUser.elo,
      gamesPlayed: apiUser.gamesPlayed,
      wins: apiUser.wins,
      losses: apiUser.losses,
      draws: apiUser.draws,
      avatarUrl: apiUser.avatarUrl,
      disc: apiUser.equippedDisc ? {
        ...apiUser.equippedDisc,
        type: apiUser.equippedDisc.type as 'color' | 'image'
      } : null
    };
  }
}