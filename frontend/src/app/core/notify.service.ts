import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Problem } from './models';

/** Traduit les erreurs de l'API en messages lisibles et les affiche. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0) {
      return "Le serveur est injoignable. Vérifiez que l'API est démarrée.";
    }
    const problem = error.error as Problem | null;
    return problem?.detail ?? problem?.title ?? `Erreur ${error.status}`;
  }
  return 'Une erreur inattendue est survenue.';
}

@Injectable({ providedIn: 'root' })
export class NotifyService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 3000, panelClass: 'snack-success' });
  }

  error(error: unknown): void {
    this.snackBar.open(errorMessage(error), 'Fermer', { duration: 6000, panelClass: 'snack-error' });
  }
}
