import { CommonModule } from "@angular/common";
import { Component, OnInit, inject } from "@angular/core";
import { Observable, of } from "rxjs";
import { UserProfile } from "../profile/profile.model";
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent implements OnInit {

  ngOnInit(): void {}

}