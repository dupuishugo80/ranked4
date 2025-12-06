import { CommonModule } from "@angular/common";
import { Component, OnInit, inject } from "@angular/core";
import { map, Observable, of } from "rxjs";
import { UserProfile } from "../../profile/profile.model";
import { ApiLootboxesResponse, Lootbox } from "./admin-lootboxes.model";
import { AdminLootboxesService } from "./admin-lootboxes.service";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-lootboxes.component.html',
  styleUrls: ['./admin-lootboxes.component.scss']
})
export class AdminLootboxesComponent implements OnInit {

  private adminLootboxesService = inject(AdminLootboxesService);

  public lootboxes$!: Observable<Lootbox[]>;

  ngOnInit(): void {
    this.loadLootboxes();
  }

  private loadLootboxes(): void {
    this.lootboxes$ = this.adminLootboxesService.getLootboxes().pipe(
      map((response: ApiLootboxesResponse) => response.content)
    );
  }
}