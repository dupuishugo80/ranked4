import { CommonModule } from "@angular/common";
import { Component, OnInit, inject } from "@angular/core";
import { Observable } from "rxjs";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-skins.component.html',
  styleUrl: './admin-skins.component.scss'
})
export class AdminSkinsComponent implements OnInit {
  ngOnInit(): void {}
}