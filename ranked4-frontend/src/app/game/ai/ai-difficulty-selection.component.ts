import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AiGameService } from './ai-game.service';

@Component({
  selector: 'app-ai-difficulty-selection',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ai-difficulty-selection.component.html',
  styleUrl: './ai-difficulty-selection.component.scss'
})
export class AiDifficultySelectionComponent {
  private aiGameService = inject(AiGameService);
  private router = inject(Router);

  public isCreating = false;
  public error: string | null = null;

  difficulties = [
    {
      level: 1,
      name: 'Easy',
      description: 'Perfect for beginners and practice',
      goldOnWin: 50,
      goldOnDraw: 25
    },
    {
      level: 2,
      name: 'Medium',
      description: 'A balanced challenge',
      goldOnWin: 100,
      goldOnDraw: 50
    },
    {
      level: 3,
      name: 'Hard',
      description: 'For experienced players only',
      goldOnWin: 200,
      goldOnDraw: 100
    }
  ];

  selectDifficulty(difficulty: number): void {
    if (this.isCreating) return;

    this.isCreating = true;
    this.error = null;

    this.aiGameService.createAiGame(difficulty).subscribe({
      next: () => {},
      error: (err) => {
        console.error('[AI] Error creating game:', err);
        this.error = 'Failed to create AI game. Please try again.';
        this.isCreating = false;
      }
    });
  }
}
